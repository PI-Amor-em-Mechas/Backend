"""Blueprint de enrollment / status de biometria de voz (Resemblyzer)."""
from __future__ import annotations

import base64
import logging

from flask import Blueprint, jsonify, request

from .. import config
from .. import db
from ..security import require_admin_profile, require_any_profile
from ..services import voice_engine

LOGGER = logging.getLogger(__name__)

bp = Blueprint("voice_biometry", __name__)

_NOT_FOUND_MSG = "Colaborador nao encontrado."


def _decode_pcm(payload: dict) -> bytes:
    """Aceita PCM16-LE como base64 (`pcm_base64`) ou lista de inteiros (`pcm`)."""
    raw = payload.get("pcm_base64")
    if isinstance(raw, str) and raw:
        try:
            return base64.b64decode(raw, validate=True)
        except Exception as exc:
            raise ValueError(f"pcm_base64 invalido: {exc}") from exc

    arr = payload.get("pcm")
    if isinstance(arr, list):
        try:
            return bytes(arr)
        except Exception as exc:
            raise ValueError(f"pcm invalido: {exc}") from exc

    raise ValueError("Forneca 'pcm_base64' (string) ou 'pcm' (lista de bytes).")


@bp.get("/voice-biometry/status/<employee_id>")
@require_any_profile
def voice_biometry_status(employee_id: str):
    if not db.get_employee(employee_id):
        return jsonify({"status": "error", "message": _NOT_FOUND_MSG}), 404
    count = db.count_voice_embeddings(employee_id)
    return jsonify({
        "status": "ok",
        "employee_id": employee_id,
        "enrolled_samples": count,
        "min_samples": config.VOICE_BIOMETRY_MIN_ENROLL_SAMPLES,
        "ready": count >= config.VOICE_BIOMETRY_MIN_ENROLL_SAMPLES,
        "threshold": config.VOICE_BIOMETRY_THRESHOLD,
        "enforced": config.VOICE_BIOMETRY_ENFORCE,
    })


@bp.post("/voice-biometry/enroll")
@require_admin_profile
def voice_biometry_enroll():
    """Adiciona uma amostra de voz ao cadastro do colaborador.

    Body JSON:
      - employee_id: str (obrigatorio)
      - pcm_base64: str (PCM16-LE mono @ 16 kHz, base64) **ou**
      - pcm: list[int] (mesmos bytes como array)
    """
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()
    if not employee_id:
        return jsonify({"status": "error", "message": "Informe employee_id."}), 400

    employee = db.get_employee(employee_id)
    if not employee:
        return jsonify({"status": "error", "message": _NOT_FOUND_MSG}), 404
    if employee.get("anonymized_at"):
        return jsonify({
            "status": "error",
            "message": "Colaborador anonimizado nao pode receber novos voiceprints.",
        }), 400

    try:
        pcm_bytes = _decode_pcm(payload)
    except ValueError as exc:
        return jsonify({"status": "error", "message": str(exc)}), 400

    try:
        info = voice_engine.enroll_sample(employee_id, pcm_bytes)
    except ValueError as exc:
        return jsonify({"status": "error", "message": str(exc)}), 400
    except Exception as exc:
        LOGGER.exception("Falha no enrollment de voz: %s", exc)
        return jsonify({
            "status": "error",
            "message": f"Falha ao calcular voiceprint: {exc}",
        }), 500

    total = db.count_voice_embeddings(employee_id)
    db.add_audit(
        actor="admin",
        action="voice.enroll",
        target=employee_id,
        ip=request.remote_addr,
        details=(
            f"id={info['id']} duration={info['duration_seconds']}s "
            f"total_samples={total}"
        ),
    )
    return jsonify({
        "status": "enrolled",
        "message": "Amostra de voz cadastrada com sucesso.",
        "embedding_id": info["id"],
        "duration_seconds": info["duration_seconds"],
        "total_samples": total,
        "ready": total >= config.VOICE_BIOMETRY_MIN_ENROLL_SAMPLES,
    })


@bp.post("/voice-biometry/clear")
@require_admin_profile
def voice_biometry_clear():
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()
    if not employee_id:
        return jsonify({"status": "error", "message": "Informe employee_id."}), 400
    if not db.get_employee(employee_id):
        return jsonify({"status": "error", "message": _NOT_FOUND_MSG}), 404

    removed = db.delete_voice_embeddings(employee_id)
    db.add_audit(
        actor="admin",
        action="voice.clear",
        target=employee_id,
        ip=request.remote_addr,
        details=f"removed={removed}",
    )
    return jsonify({
        "status": "cleared",
        "message": f"{removed} voiceprint(s) removido(s).",
        "removed": removed,
    })
