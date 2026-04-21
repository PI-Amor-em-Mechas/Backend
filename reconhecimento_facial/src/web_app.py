"""Aplicacao Flask (app factory) + handlers SocketIO de voz.

Executar com:  `python -m src.web_app`
"""
from __future__ import annotations

import logging
import socket
import threading
from datetime import datetime, timedelta, timezone

from flask import Flask, request
from flask_socketio import SocketIO, disconnect as ws_disconnect, emit

from . import config
from . import db
from .routes import auth as auth_bp
from .routes import employees as employees_bp
from .routes import lgpd as lgpd_bp
from .routes import recognition as recognition_bp
from .routes import voice_phrases as voice_phrases_bp
from .services.lgpd import apply_retention_policy
from .voice.state import VoiceSession
from .voice.training import add_phrase, load_phrases
from .voice.vosk_engine import create_recognizer, feed_audio, finalize

LOGGER = logging.getLogger(__name__)

socketio: SocketIO | None = None

_voice_sessions: dict[str, VoiceSession] = {}

KEYWORD = "salvar"
VOICE_NOT_AUTH_MSG = "Sessao de voz nao autenticada."

def _configure_logging() -> None:
    level = getattr(logging, config.LOG_LEVEL.upper(), logging.INFO)
    logging.basicConfig(
        level=level,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    )

def create_app() -> tuple[Flask, SocketIO]:
    global socketio

    app = Flask(
        __name__,
        template_folder="templates",
        static_folder="static",
    )
    app.config["SECRET_KEY"] = config.load_or_create_secret_key()

    app.config.update(
        SESSION_COOKIE_HTTPONLY=True,
        SESSION_COOKIE_SAMESITE="Lax",
    )

    app.register_blueprint(auth_bp.bp)
    app.register_blueprint(employees_bp.bp)
    app.register_blueprint(recognition_bp.bp)
    app.register_blueprint(voice_phrases_bp.bp)
    app.register_blueprint(lgpd_bp.bp)

    async_mode = "gevent"
    try:
        import gevent  # type: ignore # noqa: F401
    except Exception:
        async_mode = "threading"

    socketio = SocketIO(
        app,
        async_mode=async_mode,
        ping_interval=25,
        ping_timeout=60,
        cors_allowed_origins=[],
    )

    _register_voice_handlers(socketio)
    return app, socketio

def _build_voice_recognizer():
    return create_recognizer(
        sample_rate=config.VOICE_SAMPLE_RATE,
        phrases=load_phrases(),
    )

def _cleanup_voice_auth() -> None:
    auth_lock, auth_map = recognition_bp.get_voice_auth_state()
    expiry = datetime.now(timezone.utc) - timedelta(minutes=10)
    with auth_lock:
        expired = [t for t, v in auth_map.items() if v["created_at"] < expiry]
        for t in expired:
            auth_map.pop(t, None)

def _check_and_save(session: VoiceSession, new_text: str) -> dict | None:
    stripped = new_text.strip()
    if not stripped.endswith(KEYWORD):
        return None
    text_part = stripped[: -len(KEYWORD)].strip()
    combined = (
        (session.accumulated_text + " " + text_part).strip()
        if session.accumulated_text
        else text_part
    )
    if not combined:
        session.accumulated_text = ""
        session.partial_text = ""
        return {"empty": True}

    cmd_id = db.add_voice_command(session.user_id, combined)
    add_phrase(combined)
    session.accumulated_text = ""
    session.partial_text = ""
    return {"id": cmd_id, "text": combined, "empty": False}

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

def _register_voice_handlers(sio: SocketIO) -> None:
    @sio.on("voice_auth")
    def handle_voice_auth(data):
        _cleanup_voice_auth()
        token = data.get("token") if isinstance(data, dict) else None
        if not token:
            emit("voice_error", {"message": "Token de voz ausente."})
            ws_disconnect()
            return

        auth_lock, auth_map = recognition_bp.get_voice_auth_state()
        with auth_lock:
            auth = auth_map.get(token)

        if not auth:
            emit("voice_error", {"message": "Token de voz invalido ou expirado."})
            ws_disconnect()
            return

        sid = request.sid  # type: ignore[attr-defined]
        _voice_sessions.pop(sid, None)
        phrases = load_phrases()
        LOGGER.info(
            "Voice session started for user %s (sid=%s) — grammar: %d phrases",
            auth["employee_id"], sid, len(phrases),
        )
        _voice_sessions[sid] = VoiceSession(
            user_id=auth["employee_id"],
            user_name=auth["name"],
            recognizer=_build_voice_recognizer(),
        )
        emit("voice_ready", {"message": "Pronto para receber audio.", "phrases": len(phrases)})

    @sio.on("audio_chunk")
    def handle_audio_chunk(data):
        try:
            sid = request.sid  # type: ignore[attr-defined]
            session = _voice_sessions.get(sid)
            if not session:
                emit("voice_error", {"message": VOICE_NOT_AUTH_MSG})
                return

            if isinstance(data, (bytes, bytearray, memoryview)):
                audio_bytes = bytes(data)
            elif isinstance(data, list):
                audio_bytes = bytes(data)
            else:
                emit("voice_error", {"message": "Formato de audio invalido."})
                return

            if not audio_bytes:
                return

            final_text, partial_text = feed_audio(session.recognizer, audio_bytes)

            if final_text is not None:
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

    @sio.on("force_save")
    def handle_force_save():
        sid = request.sid  # type: ignore[attr-defined]
        session = _voice_sessions.get(sid)
        if not session:
            emit("voice_error", {"message": VOICE_NOT_AUTH_MSG})
            return

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
            session.recognizer = _build_voice_recognizer()
            return

        cmd_id = db.add_voice_command(session.user_id, combined)
        add_phrase(combined)
        session.accumulated_text = ""
        session.partial_text = ""
        session.recognizer = _build_voice_recognizer()

        emit("voice_saved", {
            "message": "Comando salvo com sucesso.",
            "text": combined,
            "id": cmd_id,
        })

    @sio.on("voice_train")
    def handle_voice_train(data):
        sid = request.sid  # type: ignore[attr-defined]
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
            sample_rate=config.VOICE_SAMPLE_RATE, phrases=phrases,
        )
        session.partial_text = ""
        emit("voice_trained", {
            "message": "Treino de voz atualizado com sucesso.",
            "phrase": phrase,
            "phrases": len(phrases),
        })

    @sio.on("disconnect")
    def handle_disconnect():
        sid = request.sid  # type: ignore[attr-defined]
        session = _voice_sessions.pop(sid, None)
        if session:
            LOGGER.info("Voice session ended for user %s (sid=%s)", session.user_id, sid)

def _start_retention_scheduler() -> None:
    """Dispara retencao a cada 6h em uma thread daemon."""
    def _loop():
        interval = 6 * 60 * 60
        while True:
            try:
                apply_retention_policy()
            except Exception:
                LOGGER.exception("Falha ao aplicar retencao periodica")
            threading.Event().wait(interval)

    thread = threading.Thread(target=_loop, name="lgpd-retention", daemon=True)
    thread.start()

def main() -> None:
    _configure_logging()
    config.ensure_directories()
    db.init_db()

    if not config.ADMIN_PROFILE_PASSWORD:
        LOGGER.warning(
            "ADMIN_PROFILE_PASSWORD nao definido. Login admin esta desabilitado. "
            "Defina a variavel de ambiente ADMIN_PROFILE_PASSWORD."
        )

    app, sio = create_app()

    try:
        apply_retention_policy()
    except Exception:
        LOGGER.exception("Falha ao aplicar politica de retencao LGPD no boot")
    _start_retention_scheduler()

    LOGGER.info("SocketIO async_mode: %s", sio.async_mode)

    host = "127.0.0.1"
    port = 5000
    while True:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            if sock.connect_ex((host, port)) != 0:
                break
        LOGGER.warning("Porta %s em uso. Tentando %s...", port, port + 1)
        port += 1

    LOGGER.info("Servidor iniciando em http://%s:%s", host, port)
    sio.run(app, host=host, port=port, debug=False, allow_unsafe_werkzeug=True)

if __name__ == "__main__":
    main()
