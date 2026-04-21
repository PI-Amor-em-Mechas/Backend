"""Blueprint de gestao de colaboradores (admin) — YuNet + SFace."""
from __future__ import annotations

import cv2
from flask import Blueprint, jsonify, render_template, request

from .. import config
from .. import db
from ..security import require_admin_profile
from ..services.face_engine import (
    compute_embedding,
    detect_largest_face,
    encode_embedding,
    invalidate_index,
    rebuild_index_from_dataset,
)
from ..services.frames import decode_frame_from_data_uri
from ..services.lgpd import has_valid_consent, record_consent
from ..services.model_cache import train_lock

bp = Blueprint("employees", __name__)

@bp.get("/register-window")
@require_admin_profile
def register_window():
    return render_template("register_window.html")

@bp.get("/employees")
@require_admin_profile
def employees_list():
    employees = db.list_employees()
    for item in employees:
        person_dir = config.DATASET_DIR / item["id"]
        if person_dir.exists() and person_dir.is_dir():
            count = len([
                p for p in person_dir.iterdir()
                if p.suffix.lower() in {".png", ".jpg", ".jpeg"}
            ])
        else:
            count = 0
        item["sample_count"] = count
        item["embedding_count"] = db.count_face_embeddings(item["id"])
        item["consent_valid"] = has_valid_consent(item["id"])
    return jsonify({"status": "ok", "employees": employees})

@bp.post("/employees/update")
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
    db.add_audit(actor="admin", action="employee.update", target=employee_id, ip=request.remote_addr)

    return jsonify({
        "status": "updated",
        "message": "Usuario atualizado com sucesso.",
        "employee_id": employee_id,
        "employee_name": employee_name,
    })

@bp.post("/employees/delete")
@require_admin_profile
def employee_delete():
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()
    if not employee_id:
        return jsonify({"status": "error", "message": "Informe o ID do usuario."}), 400
    if not db.get_employee(employee_id):
        return jsonify({"status": "error", "message": "Usuario nao encontrado."}), 404

    from ..services.lgpd import erase_employee_data

    result = erase_employee_data(employee_id, actor="admin", ip=request.remote_addr)
    invalidate_index()

    return jsonify({
        "status": "deleted",
        "message": "Usuario removido e dados anonimizados.",
        "employee_id": employee_id,
        "details": result,
    })

def _save_sample_and_embed(employee_id: str, image_data: str, idx: int) -> bool:
    """Decodifica, detecta, alinha, salva crop BGR 112x112 e embedding.

    Retorna True se uma amostra foi gravada com sucesso.
    """
    try:
        frame = decode_frame_from_data_uri(image_data)
    except Exception:
        return False
    face = detect_largest_face(frame)
    if face is None:
        return False

    person_dir = config.DATASET_DIR / employee_id
    image_path = person_dir / f"{employee_id}_{idx:04d}.png"
    if not cv2.imwrite(str(image_path), face.aligned_crop):
        return False

    try:
        vec = compute_embedding(face)
    except Exception:
        image_path.unlink(missing_ok=True)
        return False

    db.add_face_embedding(
        employee_id=employee_id,
        vec_bytes=encode_embedding(vec),
        dim=vec.shape[0],
        dtype="float32",
    )
    return True

@bp.post("/register-person")
@require_admin_profile
def register_person():
    payload = request.get_json(silent=True) or {}
    employee_id = str(payload.get("employee_id", "")).strip()
    employee_name = str(payload.get("employee_name", "")).strip()
    images = payload.get("images") or []
    consent = bool(payload.get("consent", False))

    if not employee_id or not employee_name:
        return jsonify({"status": "error", "message": "Informe ID e nome da pessoa."}), 400
    if not isinstance(images, list) or not images:
        return jsonify({"status": "error", "message": "Capture pelo menos uma foto para cadastro."}), 400
    if not consent:
        return jsonify({
            "status": "error",
            "message": "Consentimento LGPD obrigatorio para cadastro biometrico.",
        }), 400

    person_dir = config.DATASET_DIR / employee_id
    person_dir.mkdir(parents=True, exist_ok=True)

    saved = sum(1 for idx, img in enumerate(images)
                if _save_sample_and_embed(employee_id, img, idx))

    if saved == 0:
        return jsonify({
            "status": "error",
            "message": "Nao foi possivel detectar um rosto valido nas fotos capturadas.",
        }), 400

    db.add_employee(employee_id, employee_name)
    record_consent(
        employee_id,
        user_agent=request.headers.get("User-Agent"),
        ip=request.remote_addr,
        actor="admin",
    )
    db.add_audit(
        actor="admin",
        action="employee.register",
        target=employee_id,
        ip=request.remote_addr,
        details=f"samples={saved}",
    )
    invalidate_index()

    return jsonify({
        "status": "registered",
        "message": f"Pessoa cadastrada com sucesso com {saved} fotos.",
        "saved_samples": saved,
        "employee_id": employee_id,
        "employee_name": employee_name,
    })

@bp.post("/train-model")
@require_admin_profile
def retrain_model():
    """Recalcula embeddings a partir do dataset/ (substitui o treino LBPH)."""
    with train_lock():
        try:
            stats = rebuild_index_from_dataset()
        except Exception as exc:
            return jsonify({"status": "error", "message": f"Falha no rebuild: {exc}"}), 500
    db.add_audit(actor="admin", action="model.rebuild", ip=request.remote_addr,
                 details=str(stats))
    return jsonify({
        "status": "trained",
        "message": "Indice de embeddings reconstruido.",
        "stats": stats,
    })
