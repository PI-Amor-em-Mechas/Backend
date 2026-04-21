"""Blueprint de direitos do titular e conformidade LGPD."""
from __future__ import annotations

import io
import json

from flask import Blueprint, jsonify, request, send_file

from .. import db
from ..security import require_admin_profile, require_any_profile
from ..services.lgpd import (
    apply_retention_policy,
    erase_employee_data,
    export_employee_data,
    privacy_notice,
    record_consent,
    revoke_consent,
)

bp = Blueprint("lgpd", __name__, url_prefix="/lgpd")

@bp.get("/privacy-notice")
def privacy_notice_view():
    return jsonify({"status": "ok", **privacy_notice()})

@bp.post("/consent/<employee_id>")
@require_admin_profile
def grant_consent(employee_id: str):
    if not db.get_employee(employee_id):
        return jsonify({"status": "error", "message": "Colaborador nao encontrado."}), 404
    cid = record_consent(
        employee_id,
        user_agent=request.headers.get("User-Agent"),
        ip=request.remote_addr,
        actor="admin",
    )
    return jsonify({"status": "ok", "consent_id": cid})

@bp.post("/consent/<employee_id>/revoke")
@require_admin_profile
def revoke(employee_id: str):
    if not db.get_employee(employee_id):
        return jsonify({"status": "error", "message": "Colaborador nao encontrado."}), 404
    count = revoke_consent(employee_id, actor="admin", ip=request.remote_addr)
    return jsonify({"status": "ok", "revoked_count": count})

@bp.get("/consent/<employee_id>")
@require_admin_profile
def list_consent(employee_id: str):
    return jsonify({
        "status": "ok",
        "latest": db.latest_consent(employee_id),
        "history": db.list_consents(employee_id),
    })

@bp.get("/export/<employee_id>")
@require_admin_profile
def export(employee_id: str):
    try:
        data = export_employee_data(employee_id)
    except ValueError as exc:
        return jsonify({"status": "error", "message": str(exc)}), 404
    payload = json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")
    return send_file(
        io.BytesIO(payload),
        mimetype="application/json",
        as_attachment=True,
        download_name=f"lgpd_export_{employee_id}.json",
    )

@bp.post("/erase/<employee_id>")
@require_admin_profile
def erase(employee_id: str):
    try:
        result = erase_employee_data(employee_id, actor="admin", ip=request.remote_addr)
    except ValueError as exc:
        return jsonify({"status": "error", "message": str(exc)}), 404
    return jsonify({"status": "ok", "result": result})

@bp.post("/retention/apply")
@require_admin_profile
def retention_apply():
    result = apply_retention_policy()
    return jsonify({"status": "ok", "result": result})

@bp.get("/audit")
@require_admin_profile
def audit_log():
    try:
        limit = int(request.args.get("limit", "200"))
    except ValueError:
        limit = 200
    return jsonify({"status": "ok", "items": db.list_audit(limit=limit)})
