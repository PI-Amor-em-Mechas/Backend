import base64
import io
import json
import logging
import os
import secrets
import socket
import sys
import threading
from datetime import datetime, timedelta, timezone
from functools import wraps
from pathlib import Path

import cv2
import numpy as np
from flask import Flask, jsonify, render_template, request, session, redirect, url_for, send_file
from flask_socketio import SocketIO, emit, disconnect as ws_disconnect

PROJECT_ROOT = Path(__file__).resolve().parent.parent
PROJECT_PARENT = PROJECT_ROOT.parent
if str(PROJECT_PARENT) not in sys.path:
    sys.path.insert(0, str(PROJECT_PARENT))

import reconhecimento_facial.src.config as config
import reconhecimento_facial.src.db as db
from reconhecimento_facial.src.recognize import (
    _is_within_cooldown,
    _load_model_and_labels,
    _next_punch_type,
)
from reconhecimento_facial.src.train import train_model
from reconhecimento_facial.src.utils import create_face_detector, detect_largest_face, preprocess_face, utc_now_iso
from reconhecimento_facial.src.voice.vosk_engine import create_recognizer, feed_audio, finalize
from reconhecimento_facial.src.voice.state import VoiceSession
from reconhecimento_facial.src.voice.training import add_phrase, add_phrases, load_phrases, remove_phrase

LOGGER = logging.getLogger(__name__)
APP_DIR = Path(__file__).resolve().parent

app = Flask(
    __name__,
    template_folder=str(APP_DIR / "templates"),
    static_folder=str(APP_DIR / "static"),
)
app.config["SECRET_KEY"] = secrets.token_hex(32)
ADMIN_PROFILE_PASSWORD = os.getenv("ADMIN_PROFILE_PASSWORD", config.ADMIN_PROFILE_PASSWORD)

_socketio_async_mode = "gevent"
try:
    import gevent  # type: ignore # noqa: F401
except Exception:
    _socketio_async_mode = "threading"

socketio = SocketIO(
    app,
    async_mode=_socketio_async_mode,
    ping_interval=25,
    ping_timeout=60,
)

_model_lock = threading.Lock()
_detector_lock = threading.Lock()
_camera_lock = threading.Lock()
_pending_lock = threading.Lock()
_train_lock = threading.Lock()

_recognizer = None
_labels = None
_detector = None
_pending_confirmations: dict[str, dict] = {}

# --- Voice auth state ---
_voice_auth_lock = threading.Lock()
_voice_auth: dict[str, dict] = {}      # voice_token -> {employee_id, name, created_at}
_voice_sessions: dict[str, VoiceSession] = {}  # socket sid -> VoiceSession

PROFILE_ADMIN = "admin"
PROFILE_DEFAULT = "default"


def _current_profile() -> str | None:
    profile = session.get("profile")
    if profile in {PROFILE_ADMIN, PROFILE_DEFAULT}:
        return str(profile)
    return None


def _forbidden_json(message: str = "Acesso negado."):
    return jsonify({"status": "forbidden", "message": message}), 403


def require_any_profile(view):
    @wraps(view)
    def wrapper(*args, **kwargs):
        if _current_profile() is None:
            if request.method == "GET" and request.path.endswith("-window"):
                return redirect(url_for("login_page"))
            return _forbidden_json("Selecione um perfil antes de continuar.")
        return view(*args, **kwargs)

    return wrapper


def require_admin_profile(view):
    @wraps(view)
    def wrapper(*args, **kwargs):
        profile = _current_profile()
        if profile is None:
            if request.method == "GET" and request.path.endswith("-window"):
                return redirect(url_for("login_page"))
            return _forbidden_json("Selecione um perfil antes de continuar.")

        if profile != PROFILE_ADMIN:
            if request.method == "GET" and request.path.endswith("-window"):
                return redirect(url_for("login_page"))
            return _forbidden_json("Somente perfil admin pode executar esta acao.")

        return view(*args, **kwargs)

    return wrapper



def _configure_logging() -> None:
    level = getattr(logging, config.LOG_LEVEL.upper(), logging.INFO)
    logging.basicConfig(
        level=level,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    )



def _cleanup_pending() -> None:
    expiry = datetime.now(timezone.utc) - timedelta(minutes=2)
    remove_tokens: list[str] = []

    with _pending_lock:
        for token, item in _pending_confirmations.items():
            created_at = item.get("created_at")
            if created_at and created_at < expiry:
                image_path = item.get("image_path")
                if image_path:
                    Path(image_path).unlink(missing_ok=True)
                remove_tokens.append(token)

        for token in remove_tokens:
            _pending_confirmations.pop(token, None)



def _get_model_and_labels():
    global _recognizer, _labels
    with _model_lock:
        if _recognizer is None or _labels is None:
            _recognizer, _labels = _load_model_and_labels()
    return _recognizer, _labels


def _reset_model_cache() -> None:
    global _recognizer, _labels
    with _model_lock:
        _recognizer = None
        _labels = None



def _get_detector():
    global _detector
    with _detector_lock:
        if _detector is None:
            _detector = create_face_detector()
    return _detector



def _capture_frame() -> any:
    with _camera_lock:
        cap = cv2.VideoCapture(config.CAMERA_INDEX)
        if not cap.isOpened():
            raise RuntimeError("Nao foi possivel abrir a webcam.")

        cap.set(cv2.CAP_PROP_FRAME_WIDTH, config.FRAME_WIDTH)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, config.FRAME_HEIGHT)

        frame = None
        for _ in range(5):
            ok, current_frame = cap.read()
            if ok:
                frame = current_frame

        cap.release()

        if frame is None:
            raise RuntimeError("Nao foi possivel capturar imagem da webcam.")

        return frame



def _save_pending_face(employee_id: str, face_bgr) -> str | None:
    if not config.SAVE_PUNCH_IMAGE:
        return None

    token = secrets.token_hex(8)
    filename = f"pending_{employee_id}_{token}.png"
    path = config.PUNCH_IMAGES_DIR / filename
    ok = cv2.imwrite(str(path), face_bgr)
    return str(path) if ok else None



def _to_data_uri(face_bgr) -> str:
    ok, buffer = cv2.imencode(".jpg", face_bgr)
    if not ok:
        raise RuntimeError("Falha ao codificar imagem de rosto.")
    b64 = base64.b64encode(buffer.tobytes()).decode("ascii")
    return f"data:image/jpeg;base64,{b64}"


def _to_data_uri_from_file(image_path: Path) -> str | None:
    image = cv2.imread(str(image_path), cv2.IMREAD_UNCHANGED)
    if image is None:
        return None
    return _to_data_uri(image)


def _get_reference_image(employee_id: str, fallback_face) -> str:
    person_dir = config.DATASET_DIR / employee_id
    if person_dir.exists() and person_dir.is_dir():
        sample_files = sorted(
            p
            for p in person_dir.iterdir()
            if p.suffix.lower() in {".png", ".jpg", ".jpeg"}
        )
        for sample in sample_files:
            data_uri = _to_data_uri_from_file(sample)
            if data_uri:
                return data_uri

    return _to_data_uri(fallback_face)


def _refresh_model_after_dataset_change() -> tuple[bool, str]:
    with _train_lock:
        try:
            train_model()
            _reset_model_cache()
            _get_model_and_labels()
            return True, "Modelo atualizado com sucesso."
        except Exception as exc:
            # Quando nao ha mais imagens no dataset, remove o modelo para evitar estado inconsistente.
            if "No training samples found" in str(exc):
                config.LBPH_MODEL_PATH.unlink(missing_ok=True)
                config.LABELS_PATH.unlink(missing_ok=True)
                _reset_model_cache()
                return True, "Nao ha mais amostras para treino. Modelo removido."
            return False, str(exc)


def _update_name_in_labels_file(employee_id: str, employee_name: str) -> None:
    if not config.LABELS_PATH.exists():
        return

    with config.LABELS_PATH.open("r", encoding="utf-8") as f:
        payload = json.load(f)

    names = payload.get("employee_id_to_name", {})
    if not isinstance(names, dict):
        names = {}

    names[employee_id] = employee_name
    payload["employee_id_to_name"] = names

    with config.LABELS_PATH.open("w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)

    with _model_lock:
        if _labels is not None:
            current = _labels.get("employee_id_to_name", {})
            if isinstance(current, dict):
                current[employee_id] = employee_name



@app.get("/login")
def login_page():
    if _current_profile() is not None:
        return redirect(url_for("index"))
    return render_template("login.html")


@app.get("/")
def index():
    if _current_profile() is None:
        return redirect(url_for("login_page"))
    return render_template("index.html")


@app.get("/me")
def me():
    profile = _current_profile()
    return jsonify({"status": "ok", "profile": profile})


@app.post("/set-profile")
def set_profile():
    payload = request.get_json(silent=True) or {}
    profile = str(payload.get("profile", "")).strip().lower()
    password = str(payload.get("password", ""))

    if profile == PROFILE_DEFAULT:
        session["profile"] = PROFILE_DEFAULT
        return jsonify({"status": "ok", "profile": PROFILE_DEFAULT})

    if profile == PROFILE_ADMIN:
        if password != ADMIN_PROFILE_PASSWORD:
            return jsonify({"status": "error", "message": "Senha admin invalida."}), 401
        session["profile"] = PROFILE_ADMIN
        return jsonify({"status": "ok", "profile": PROFILE_ADMIN})

    return jsonify({"status": "error", "message": "Perfil invalido."}), 400


@app.post("/logout")
def logout():
    session.pop("profile", None)
    if request.is_json:
        return jsonify({"status": "ok"})
    return redirect(url_for("login_page"))


@app.get("/recognition-window")
@require_any_profile
def recognition_window():
    return render_template("recognition_window.html")


@app.get("/register-window")
@require_admin_profile
def register_window():
    return render_template("register_window.html")


@app.get("/employees")
@require_admin_profile
def employees_list():
    employees = db.list_employees()
    for item in employees:
        person_dir = config.DATASET_DIR / item["id"]
        if person_dir.exists() and person_dir.is_dir():
            count = len(
                [
                    p
                    for p in person_dir.iterdir()
                    if p.suffix.lower() in {".png", ".jpg", ".jpeg"}
                ]
            )
        else:
            count = 0
        item["sample_count"] = count
    return jsonify({"status": "ok", "employees": employees})


@app.post("/employees/update")
@require_admin_profile
def employee_update():
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()
    employee_name = str(payload.get("employee_name", "")).strip()

    if not employee_id or not employee_name:
        return jsonify({"status": "error", "message": "Informe ID e novo nome."}), 400

    if not db.get_employee(employee_id):
        return jsonify({"status": "error", "message": "Usuario nao encontrado."}), 404

    db.add_employee(employee_id, employee_name)
    _update_name_in_labels_file(employee_id, employee_name)

    return jsonify(
        {
            "status": "updated",
            "message": "Usuario atualizado com sucesso.",
            "employee_id": employee_id,
            "employee_name": employee_name,
        }
    )


@app.post("/employees/delete")
@require_admin_profile
def employee_delete():
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()

    if not employee_id:
        return jsonify({"status": "error", "message": "Informe o ID do usuario."}), 400

    existing = db.get_employee(employee_id)
    if not existing:
        return jsonify({"status": "error", "message": "Usuario nao encontrado."}), 404

    person_dir = config.DATASET_DIR / employee_id
    if person_dir.exists() and person_dir.is_dir():
        for image in person_dir.iterdir():
            if image.is_file():
                image.unlink(missing_ok=True)
        person_dir.rmdir()

    db.delete_employee(employee_id)

    ok, refresh_message = _refresh_model_after_dataset_change()
    if not ok:
        return jsonify(
            {
                "status": "warning",
                "message": f"Usuario removido, mas houve falha ao atualizar modelo: {refresh_message}",
            }
        ), 500

    return jsonify(
        {
            "status": "deleted",
            "message": "Usuario removido com sucesso.",
            "model_message": refresh_message,
            "employee_id": employee_id,
        }
    )


@app.get("/voice-phrases")
@require_admin_profile
def voice_phrases_list():
    phrases = load_phrases()
    return jsonify({"status": "ok", "phrases": phrases, "count": len(phrases)})


@app.post("/voice-phrases/add")
@require_admin_profile
def voice_phrases_add():
    payload = request.get_json(silent=True) or {}
    phrase = str(payload.get("phrase", "")).strip()
    if not phrase:
        return jsonify({"status": "error", "message": "Informe uma frase para treino."}), 400

    phrases = add_phrase(phrase)
    return jsonify(
        {
            "status": "ok",
            "message": "Frase adicionada ao treino de voz.",
            "phrases": phrases,
            "count": len(phrases),
        }
    )


@app.post("/voice-phrases/remove")
@require_admin_profile
def voice_phrases_remove():
    payload = request.get_json(silent=True) or {}
    phrase = str(payload.get("phrase", "")).strip()
    if not phrase:
        return jsonify({"status": "error", "message": "Informe a frase para remover."}), 400

    phrases = remove_phrase(phrase)
    return jsonify(
        {
            "status": "ok",
            "message": "Frase removida do treino de voz.",
            "phrases": phrases,
            "count": len(phrases),
        }
    )


@app.post("/voice-phrases/bulk")
@require_admin_profile
def voice_phrases_bulk():
    payload = request.get_json(silent=True) or {}
    raw_text = str(payload.get("text", ""))
    lines = [line.strip() for line in raw_text.splitlines() if line.strip()]
    if not lines:
        return jsonify({"status": "error", "message": "Informe ao menos uma frase por linha."}), 400

    phrases = add_phrases(lines)
    return jsonify(
        {
            "status": "ok",
            "message": "Treino de voz atualizado em lote.",
            "phrases": phrases,
            "count": len(phrases),
        }
    )


@app.get("/voice-phrases/export-json")
@require_admin_profile
def voice_phrases_export_json():
    phrases = load_phrases()
    payload = json.dumps(
        {
            "phrases": phrases,
            "count": len(phrases),
            "exported_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        },
        ensure_ascii=False,
        indent=2,
    ).encode("utf-8")

    return send_file(
        io.BytesIO(payload),
        mimetype="application/json",
        as_attachment=True,
        download_name="voice_phrases.json",
    )


@app.post("/voice-phrases/import-json")
@require_admin_profile
def voice_phrases_import_json():
    if "file" not in request.files:
        return jsonify({"status": "error", "message": "Arquivo JSON nao enviado."}), 400

    file = request.files.get("file")
    if file is None or not file.filename:
        return jsonify({"status": "error", "message": "Selecione um arquivo JSON."}), 400

    try:
        raw = file.read().decode("utf-8")
        data = json.loads(raw)
    except Exception:
        return jsonify({"status": "error", "message": "JSON invalido."}), 400

    texts: list[str]
    if isinstance(data, list):
        texts = [str(item) for item in data if isinstance(item, str)]
    elif isinstance(data, dict):
        raw_phrases = data.get("phrases")
        if not isinstance(raw_phrases, list):
            return jsonify({"status": "error", "message": "JSON deve conter chave 'phrases' como lista."}), 400
        texts = [str(item) for item in raw_phrases if isinstance(item, str)]
    else:
        return jsonify({"status": "error", "message": "Formato JSON nao suportado."}), 400

    if not texts:
        return jsonify({"status": "error", "message": "Nenhuma frase valida encontrada no JSON."}), 400

    phrases = add_phrases(texts)
    return jsonify(
        {
            "status": "ok",
            "message": "Frases importadas do JSON com sucesso.",
            "phrases": phrases,
            "count": len(phrases),
        }
    )


def _recognize_from_frame(frame):
    _cleanup_pending()

    try:
        recognizer, labels = _get_model_and_labels()
    except Exception as exc:
        return jsonify({"status": "error", "message": f"Modelo nao carregado: {exc}"}), 500

    try:
        face_info = detect_largest_face(frame, _get_detector())
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500

    if not face_info:
        return jsonify({"status": "no_face", "message": "Nenhuma face detectada. Tente novamente."})

    face_crop = face_info["face_crop"]
    face_gray = preprocess_face(face_crop)
    pred_label, pred_confidence = recognizer.predict(face_gray)

    label_to_employee_id: dict[str, str] = labels.get("label_to_employee_id", {})
    employee_id_to_name: dict[str, str] = labels.get("employee_id_to_name", {})

    employee_id = label_to_employee_id.get(str(pred_label))
    known = bool(employee_id and pred_confidence <= config.LBPH_CONFIDENCE_THRESHOLD)

    if not known:
        return jsonify(
            {
                "status": "unknown",
                "message": "Rosto nao reconhecido.",
                "confidence": float(pred_confidence),
                "image": _to_data_uri(face_crop),
            }
        )

    name = employee_id_to_name.get(employee_id, employee_id)
    reference_image = _get_reference_image(employee_id, face_crop)

    if _is_within_cooldown(employee_id):
        return jsonify(
            {
                "status": "cooldown",
                "message": "Reconhecido, mas ainda esta no periodo de cooldown.",
                "name": name,
                "employee_id": employee_id,
                "confidence": float(pred_confidence),
                "image": reference_image,
            }
        )

    token = secrets.token_urlsafe(16)
    image_path = _save_pending_face(employee_id, face_crop)

    with _pending_lock:
        _pending_confirmations[token] = {
            "employee_id": employee_id,
            "name": name,
            "confidence": float(pred_confidence),
            "punch_type": _next_punch_type(employee_id),
            "image_path": image_path,
            "created_at": datetime.now(timezone.utc),
        }

    return jsonify(
        {
            "status": "recognized",
            "message": "Pessoa reconhecida. Confirme para registrar o ponto.",
            "token": token,
            "name": name,
            "employee_id": employee_id,
            "confidence": float(pred_confidence),
            "punch_type": _pending_confirmations[token]["punch_type"],
            "image": reference_image,
        }
    )


def _decode_frame_from_data_uri(data_uri: str):
    if not data_uri:
        raise ValueError("Imagem ausente para reconhecimento.")

    encoded = data_uri.split(",", 1)[1] if "," in data_uri else data_uri
    image_bytes = base64.b64decode(encoded)
    image_np = np.frombuffer(image_bytes, dtype=np.uint8)
    frame = cv2.imdecode(image_np, cv2.IMREAD_COLOR)

    if frame is None:
        raise ValueError("Nao foi possivel decodificar a imagem enviada.")

    return frame



@app.post("/recognize")
@require_any_profile
def recognize_once():
    try:
        frame = _capture_frame()
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500

    return _recognize_from_frame(frame)


@app.post("/recognize-frame")
@require_any_profile
def recognize_from_frame():
    payload = request.get_json(silent=True) or {}
    image_data = payload.get("image")

    try:
        frame = _decode_frame_from_data_uri(image_data)
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 400

    return _recognize_from_frame(frame)


@app.post("/train-model")
@require_admin_profile
def retrain_model():
    with _train_lock:
        try:
            train_model()
            _reset_model_cache()
            _get_model_and_labels()
        except Exception as exc:
            return jsonify({"status": "error", "message": f"Falha no treino: {exc}"}), 500

    return jsonify({"status": "trained", "message": "Treino concluido. Reconhecimento reiniciado."})


@app.post("/register-person")
@require_admin_profile
def register_person():
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()
    employee_name = str(payload.get("employee_name", "")).strip()
    images = payload.get("images") or []

    if not employee_id or not employee_name:
        return jsonify({"status": "error", "message": "Informe ID e nome da pessoa."}), 400

    if not isinstance(images, list) or not images:
        return jsonify({"status": "error", "message": "Capture pelo menos uma foto para cadastro."}), 400

    person_dir = config.DATASET_DIR / employee_id
    person_dir.mkdir(parents=True, exist_ok=True)

    detector = _get_detector()
    saved = 0

    for idx, image_data in enumerate(images):
        try:
            frame = _decode_frame_from_data_uri(image_data)
            face_info = detect_largest_face(frame, detector)
            if not face_info:
                continue

            face_gray = preprocess_face(face_info["face_crop"])
            image_path = person_dir / f"{employee_id}_{idx:04d}.png"
            if cv2.imwrite(str(image_path), face_gray):
                saved += 1
        except Exception:
            continue

    if saved == 0:
        return jsonify(
            {
                "status": "error",
                "message": "Nao foi possivel detectar um rosto valido nas fotos capturadas.",
            }
        ), 400

    db.add_employee(employee_id, employee_name)

    ok, refresh_message = _refresh_model_after_dataset_change()
    if not ok:
        return jsonify(
            {
                "status": "warning",
                "message": f"Pessoa cadastrada com {saved} fotos, mas o treino falhou: {refresh_message}",
                "saved_samples": saved,
            }
        ), 500

    return jsonify(
        {
            "status": "registered",
            "message": f"Pessoa cadastrada com sucesso com {saved} fotos.",
            "saved_samples": saved,
            "employee_id": employee_id,
            "employee_name": employee_name,
            "model_message": refresh_message,
        }
    )



@app.post("/confirm")
@require_any_profile
def confirm_punch():
    _cleanup_pending()
    payload = request.get_json(silent=True) or {}
    token = payload.get("token")
    confirm = bool(payload.get("confirm"))

    if not token:
        return jsonify({"status": "error", "message": "Token de confirmacao ausente."}), 400

    with _pending_lock:
        data = _pending_confirmations.pop(token, None)

    if not data:
        return jsonify({"status": "expired", "message": "Confirmacao expirada. Reconheca novamente."}), 400

    if not confirm:
        image_path = data.get("image_path")
        if image_path:
            Path(image_path).unlink(missing_ok=True)
        return jsonify({"status": "cancelled", "message": "Registro cancelado."})

    if _is_within_cooldown(data["employee_id"]):
        image_path = data.get("image_path")
        if image_path:
            Path(image_path).unlink(missing_ok=True)
        return jsonify(
            {
                "status": "cooldown",
                "message": "Registro nao salvo: colaborador ainda em cooldown.",
            }
        )

    punch_id = db.add_punch(
        employee_id=data["employee_id"],
        punch_type=data["punch_type"],
        confidence=float(data["confidence"]),
        image_path=data.get("image_path"),
        ts=utc_now_iso(),
    )

    # Gerar token de autorizacao para voz apos facial OK
    voice_token = secrets.token_urlsafe(16)
    with _voice_auth_lock:
        _voice_auth[voice_token] = {
            "employee_id": data["employee_id"],
            "name": data["name"],
            "created_at": datetime.now(timezone.utc),
        }

    return jsonify(
        {
            "status": "saved",
            "message": "Ponto registrado com sucesso.",
            "punch_id": punch_id,
            "name": data["name"],
            "employee_id": data["employee_id"],
            "punch_type": data["punch_type"],
            "voice_token": voice_token,
        }
    )



def main() -> None:
    _configure_logging()
    config.ensure_directories()
    db.init_db()

    LOGGER.info("SocketIO async_mode: %s", socketio.async_mode)

    host = "127.0.0.1"
    preferred_port = 5000

    port = preferred_port
    while True:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            is_free = sock.connect_ex((host, port)) != 0

        if is_free:
            break

        LOGGER.warning("Porta %s em uso. Tentando %s...", port, port + 1)
        port += 1

    LOGGER.info("Servidor iniciando em http://%s:%s", host, port)
    socketio.run(app, host=host, port=port, debug=False, allow_unsafe_werkzeug=True)


# ---------------------------------------------------------------------------
# SocketIO handlers — reconhecimento de voz offline (Vosk)
# ---------------------------------------------------------------------------

def _cleanup_voice_auth() -> None:
    """Remove voice tokens expirados (mais de 10 min)."""
    expiry = datetime.now(timezone.utc) - timedelta(minutes=10)
    with _voice_auth_lock:
        expired = [t for t, v in _voice_auth.items() if v["created_at"] < expiry]
        for t in expired:
            _voice_auth.pop(t, None)


KEYWORD = "salvar"
VOICE_NOT_AUTH_MSG = "Sessao de voz nao autenticada."


def _build_voice_recognizer():
    return create_recognizer(
        sample_rate=config.VOICE_SAMPLE_RATE,
        phrases=load_phrases(),
    )


def _check_and_save(session: VoiceSession, new_text: str) -> dict | None:
    """Verifica se o texto termina com a keyword 'salvar'.

    Retorna dict com dados do salvamento ou None se nao houver keyword.
    """
    stripped = new_text.strip()
    if not stripped.endswith(KEYWORD):
        return None

    # Remover a keyword do final
    text_part = stripped[: -len(KEYWORD)].strip()
    combined = (
        (session.accumulated_text + " " + text_part).strip()
        if session.accumulated_text
        else text_part
    )

    if not combined:
        # So falou "salvar" sem conteudo — nao salvar vazio
        session.accumulated_text = ""
        session.partial_text = ""
        return {"empty": True}

    cmd_id = db.add_voice_command(session.user_id, combined)
    add_phrase(combined)
    session.accumulated_text = ""
    session.partial_text = ""
    return {"id": cmd_id, "text": combined, "empty": False}


@socketio.on("voice_auth")
def handle_voice_auth(data):
    _cleanup_voice_auth()

    token = data.get("token") if isinstance(data, dict) else None
    if not token:
        emit("voice_error", {"message": "Token de voz ausente."})
        ws_disconnect()
        return

    with _voice_auth_lock:
        auth = _voice_auth.get(token)  # nao consumir — permite reconexao

    if not auth:
        emit("voice_error", {"message": "Token de voz invalido ou expirado."})
        ws_disconnect()
        return

    sid = request.sid
    _voice_sessions.pop(sid, None)  # limpar sessao anterior (reconexao)
    phrases = load_phrases()
    LOGGER.info("Voice session started for user %s (sid=%s) — grammar: %d phrases", auth["employee_id"], sid, len(phrases))
    _voice_sessions[sid] = VoiceSession(
        user_id=auth["employee_id"],
        user_name=auth["name"],
        recognizer=_build_voice_recognizer(),
    )
    emit("voice_ready", {"message": "Pronto para receber audio.", "phrases": len(phrases)})


def _emit_partial(session: VoiceSession, partial: str = "") -> None:
    emit("voice_partial", {
        "accumulated": session.accumulated_text,
        "partial": partial,
    })


def _accumulate(session: VoiceSession, text: str) -> None:
    stripped = text.strip()
    if not stripped:
        return
    session.accumulated_text = (
        (session.accumulated_text + " " + stripped).strip()
        if session.accumulated_text
        else stripped
    )


@socketio.on("audio_chunk")
def handle_audio_chunk(data):
    try:
        sid = request.sid
        session = _voice_sessions.get(sid)
        if not session:
            emit("voice_error", {"message": VOICE_NOT_AUTH_MSG})
            return

        if isinstance(data, (bytes, bytearray, memoryview)):
            audio_bytes = bytes(data)
        elif isinstance(data, list):
            audio_bytes = bytes(data)
        else:
            LOGGER.warning("audio_chunk: tipo inesperado %s (sid=%s)", type(data).__name__, sid)
            emit("voice_error", {"message": "Formato de audio invalido."})
            return

        if not audio_bytes:
            return

        final_text, partial_text = feed_audio(session.recognizer, audio_bytes)

        if final_text is not None:
            LOGGER.debug("Vosk final (sid=%s): '%s'", sid, final_text)
            result = _check_and_save(session, final_text)
            if result and result.get("empty"):
                emit("voice_error", {"message": "Nada para salvar (texto vazio)."})
            elif result:
                emit("voice_saved", {
                    "message": "Comando salvo com sucesso.",
                    "text": result["text"],
                    "id": result["id"],
                })
            else:
                _accumulate(session, final_text)
            _emit_partial(session)
            return

        if partial_text is not None:
            session.partial_text = partial_text
            _emit_partial(session, partial_text)
    except Exception as exc:
        LOGGER.exception("Falha ao processar audio_chunk: %s", exc)
        emit("voice_error", {"message": "Erro ao processar audio de voz."})


@socketio.on("force_save")
def handle_force_save():
    sid = request.sid
    session = _voice_sessions.get(sid)
    if not session:
        emit("voice_error", {"message": VOICE_NOT_AUTH_MSG})
        return

    # Extrair qualquer texto restante do recognizer
    remaining = finalize(session.recognizer).strip()
    if remaining.endswith(KEYWORD):
        remaining = remaining[: -len(KEYWORD)].strip()

    combined = (
        (session.accumulated_text + " " + remaining).strip()
        if session.accumulated_text
        else remaining
    )

    if not combined:
        emit("voice_error", {"message": "Nada para salvar (texto vazio)."})
        # Recriar recognizer (FinalResult reseta o estado interno)
        session.recognizer = _build_voice_recognizer()
        return

    cmd_id = db.add_voice_command(session.user_id, combined)
    add_phrase(combined)
    session.accumulated_text = ""
    session.partial_text = ""
    # Recriar recognizer apos FinalResult
    session.recognizer = _build_voice_recognizer()

    emit("voice_saved", {
        "message": "Comando salvo com sucesso.",
        "text": combined,
        "id": cmd_id,
    })


@socketio.on("voice_train")
def handle_voice_train(data):
    sid = request.sid
    session = _voice_sessions.get(sid)
    if not session:
        emit("voice_error", {"message": VOICE_NOT_AUTH_MSG})
        return

    phrase = ""
    if isinstance(data, dict):
        phrase = str(data.get("phrase", "")).strip()

    if not phrase:
        phrase = session.accumulated_text.strip()

    if not phrase:
        emit("voice_error", {"message": "Informe uma frase para treinar."})
        return

    phrases = add_phrase(phrase)
    session.recognizer = create_recognizer(
        sample_rate=config.VOICE_SAMPLE_RATE,
        phrases=phrases,
    )
    session.partial_text = ""

    emit(
        "voice_trained",
        {
            "message": "Treino de voz atualizado com sucesso.",
            "phrase": phrase,
            "phrases": len(phrases),
        },
    )


@socketio.on("disconnect")
def handle_disconnect():
    sid = request.sid
    session = _voice_sessions.pop(sid, None)
    if session:
        LOGGER.info("Voice session ended for user %s (sid=%s)", session.user_id, sid)



if __name__ == "__main__":
    main()
