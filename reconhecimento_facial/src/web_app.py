import base64
import logging
import secrets
import sys
import threading
from datetime import datetime, timedelta, timezone
from pathlib import Path

import cv2
import numpy as np
from flask import Flask, jsonify, render_template, request

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

LOGGER = logging.getLogger(__name__)
APP_DIR = Path(__file__).resolve().parent

app = Flask(
    __name__,
    template_folder=str(APP_DIR / "templates"),
    static_folder=str(APP_DIR / "static"),
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



@app.get("/")
def index():
    return render_template("index.html")


@app.get("/recognition-window")
def recognition_window():
    return render_template("recognition_window.html")


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
    if _is_within_cooldown(employee_id):
        return jsonify(
            {
                "status": "cooldown",
                "message": "Reconhecido, mas ainda esta no periodo de cooldown.",
                "name": name,
                "employee_id": employee_id,
                "confidence": float(pred_confidence),
                "image": _to_data_uri(face_crop),
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
            "image": _to_data_uri(face_crop),
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
def recognize_once():
    try:
        frame = _capture_frame()
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500

    return _recognize_from_frame(frame)


@app.post("/recognize-frame")
def recognize_from_frame():
    payload = request.get_json(silent=True) or {}
    image_data = payload.get("image")

    try:
        frame = _decode_frame_from_data_uri(image_data)
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 400

    return _recognize_from_frame(frame)


@app.post("/train-model")
def retrain_model():
    with _train_lock:
        try:
            train_model()
            _reset_model_cache()
            _get_model_and_labels()
        except Exception as exc:
            return jsonify({"status": "error", "message": f"Falha no treino: {exc}"}), 500

    return jsonify({"status": "trained", "message": "Treino concluido. Reconhecimento reiniciado."})



@app.post("/confirm")
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

    return jsonify(
        {
            "status": "saved",
            "message": "Ponto registrado com sucesso.",
            "punch_id": punch_id,
            "name": data["name"],
            "employee_id": data["employee_id"],
            "punch_type": data["punch_type"],
        }
    )



def main() -> None:
    _configure_logging()
    config.ensure_directories()
    db.init_db()

    app.run(host="127.0.0.1", port=5000, debug=False)



if __name__ == "__main__":
    main()
