"""Blueprint de Text-To-Speech (pyttsx3).

Endpoint usado pelo frontend para falar mensagens de orientacao ao usuario
apos o reconhecimento facial (ex.: "Ola, Maria. Diga sua atividade...").
"""
from __future__ import annotations

import io
import logging

from flask import Blueprint, jsonify, request, send_file

from ..security import require_any_profile
from ..services.tts import get_engine_info, synthesize_to_wav

LOGGER = logging.getLogger(__name__)

bp = Blueprint("tts", __name__, url_prefix="/tts")

_MAX_TEXT_LEN = 500


@bp.post("/speak")
@require_any_profile
def speak():
    payload = request.get_json(silent=True) or {}
    text = str(payload.get("text", "")).strip()

    if not text:
        return jsonify({"status": "error", "message": "Texto ausente."}), 400
    if len(text) > _MAX_TEXT_LEN:
        text = text[:_MAX_TEXT_LEN]

    try:
        rate = int(payload.get("rate", 175))
    except (TypeError, ValueError):
        rate = 175
    try:
        volume = float(payload.get("volume", 1.0))
    except (TypeError, ValueError):
        volume = 1.0

    length_scale: float | None = None
    if "length_scale" in payload:
        try:
            length_scale = float(payload["length_scale"])
        except (TypeError, ValueError):
            length_scale = None

    try:
        wav_bytes = synthesize_to_wav(
            text, rate=rate, volume=volume, length_scale=length_scale
        )
    except Exception as exc:
        LOGGER.exception("Falha na sintese TTS: %s", exc)
        return jsonify({"status": "error", "message": f"Falha no TTS: {exc}"}), 500

    return send_file(
        io.BytesIO(wav_bytes),
        mimetype="audio/wav",
        as_attachment=False,
        download_name="tts.wav",
    )


@bp.get("/info")
@require_any_profile
def info():
    return jsonify({"status": "ok", **get_engine_info()})
