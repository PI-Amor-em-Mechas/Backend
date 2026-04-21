"""Blueprint de reconhecimento e confirmacao de ponto (YuNet + SFace)."""
from __future__ import annotations

import logging
import secrets
import threading
from datetime import datetime, timedelta, timezone
from pathlib import Path

import cv2
from flask import Blueprint, jsonify, render_template, request

from .. import config
from .. import db
from ..security import require_any_profile
from ..services.face_engine import (
    compute_embedding,
    detect_largest_face,
    match_embedding,
)
from ..services.frames import (
    decode_frame_from_data_uri,
    get_reference_image,
    save_pending_face,
    to_data_uri,
)
from ..services.punch_rules import determine_punch_type, is_within_cooldown
from ..utils import utc_now_iso

LOGGER = logging.getLogger(__name__)

bp = Blueprint("recognition", __name__)

_pending_lock = threading.Lock()
_camera_lock = threading.Lock()
_pending_confirmations: dict[str, dict] = {}

_voice_auth_lock = threading.Lock()
_voice_auth: dict[str, dict] = {}

def get_voice_auth_state() -> tuple[threading.Lock, dict[str, dict]]:
    return _voice_auth_lock, _voice_auth

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

def _capture_frame():
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

def _recognize_from_frame(frame):
    _cleanup_pending()
    try:
        face = detect_largest_face(frame)
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500

    if face is None:
        return jsonify({"status": "no_face", "message": "Nenhuma face detectada. Tente novamente."})

    try:
        vec = compute_embedding(face)
        match = match_embedding(vec)
    except Exception as exc:
        return jsonify({"status": "error", "message": f"Falha no reconhecimento: {exc}"}), 500

    if not match.known:
        return jsonify({
            "status": "unknown",
            "message": "Rosto nao reconhecido.",
            "confidence": float(match.score),
            "image": to_data_uri(face.aligned_crop),
        })

    employee_id = match.employee_id or ""
    emp = db.get_employee(employee_id) or {}
    name = emp.get("name") or employee_id
    reference_image = get_reference_image(employee_id, face.aligned_crop)

    if is_within_cooldown(employee_id):
        return jsonify({
            "status": "cooldown",
            "message": "Reconhecido, mas ainda esta no periodo de cooldown.",
            "name": name,
            "employee_id": employee_id,
            "confidence": float(match.score),
            "image": reference_image,
        })

    token = secrets.token_urlsafe(16)
    image_path = save_pending_face(employee_id, face.aligned_crop)
    punch_type = determine_punch_type(employee_id)

    with _pending_lock:
        _pending_confirmations[token] = {
            "employee_id": employee_id,
            "name": name,
            "confidence": float(match.score),
            "punch_type": punch_type,
            "image_path": image_path,
            "created_at": datetime.now(timezone.utc),
        }

    return jsonify({
        "status": "recognized",
        "message": "Pessoa reconhecida. Confirme para registrar o ponto.",
        "token": token,
        "name": name,
        "employee_id": employee_id,
        "confidence": float(match.score),
        "punch_type": punch_type,
        "image": reference_image,
    })

@bp.get("/recognition-window")
@require_any_profile
def recognition_window():
    return render_template("recognition_window.html")

@bp.post("/recognize")
@require_any_profile
def recognize_once():
    try:
        frame = _capture_frame()
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500
    return _recognize_from_frame(frame)

@bp.post("/recognize-frame")
@require_any_profile
def recognize_from_frame():
    payload = request.get_json(silent=True) or {}
    try:
        frame = decode_frame_from_data_uri(payload.get("image"))
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 400
    return _recognize_from_frame(frame)

@bp.post("/confirm")
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

    if is_within_cooldown(data["employee_id"]):
        image_path = data.get("image_path")
        if image_path:
            Path(image_path).unlink(missing_ok=True)
        return jsonify({
            "status": "cooldown",
            "message": "Registro nao salvo: colaborador ainda em cooldown.",
        })

    punch_type = determine_punch_type(data["employee_id"])

    punch_id = db.add_punch(
        employee_id=data["employee_id"],
        punch_type=punch_type,
        confidence=float(data["confidence"]),
        image_path=data.get("image_path"),
        ts=utc_now_iso(),
    )

    db.add_audit(
        actor="default",
        action="punch.commit",
        target=data["employee_id"],
        ip=request.remote_addr,
        details=f"type={punch_type} id={punch_id}",
    )

    voice_token = secrets.token_urlsafe(16)
    with _voice_auth_lock:
        _voice_auth[voice_token] = {
            "employee_id": data["employee_id"],
            "name": data["name"],
            "created_at": datetime.now(timezone.utc),
        }

    return jsonify({
        "status": "saved",
        "message": "Ponto registrado com sucesso.",
        "punch_id": punch_id,
        "name": data["name"],
        "employee_id": data["employee_id"],
        "punch_type": punch_type,
        "voice_token": voice_token,
    })
