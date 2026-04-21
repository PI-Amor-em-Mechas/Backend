"""Operacoes de conformidade com a LGPD.

Inclui:
- Registro e revogacao de consentimento.
- Retencao automatica de dados (imagens de ponto e audit log).
- Exportacao de dados pessoais (direito de acesso / portabilidade).
- Apagamento (direito ao esquecimento) com anonimizacao do historico de pontos.
"""
from __future__ import annotations

import json
import logging
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from .. import config
from .. import db

LOGGER = logging.getLogger(__name__)

PRIVACY_NOTICE = (
    "Esta aplicacao trata dados biometricos (imagem facial) e eventuais "
    "comandos de voz com a finalidade exclusiva de controle de ponto. "
    "Os dados sao armazenados localmente, com acesso restrito, e podem ser "
    "removidos a qualquer momento mediante solicitacao (direito ao esquecimento). "
    "Ao cadastrar-se, voce declara ter lido o aviso de privacidade e concorda "
    "com o tratamento dos seus dados para esta finalidade."
)

def privacy_notice() -> dict[str, Any]:
    return {
        "version": config.CONSENT_VERSION,
        "text": PRIVACY_NOTICE,
        "retention_days": config.DATA_RETENTION_DAYS,
        "anonymized_retention_days": config.ANONYMIZED_PUNCHES_RETENTION_DAYS,
    }

def record_consent(
    employee_id: str,
    *,
    user_agent: str | None = None,
    ip: str | None = None,
    actor: str | None = None,
) -> int:
    cid = db.add_consent(
        employee_id=employee_id,
        version=config.CONSENT_VERSION,
        user_agent=user_agent,
        ip=ip,
    )
    db.add_audit(
        actor=actor or "system",
        action="consent.granted",
        target=employee_id,
        ip=ip,
        details=f"version={config.CONSENT_VERSION}",
    )
    return cid

def has_valid_consent(employee_id: str) -> bool:
    latest = db.latest_consent(employee_id)
    if not latest:
        return False
    if latest.get("revoked_at"):
        return False
    return latest.get("version") == config.CONSENT_VERSION

def revoke_consent(employee_id: str, *, actor: str | None = None, ip: str | None = None) -> int:
    count = db.revoke_consents(employee_id)
    db.add_audit(
        actor=actor or "system",
        action="consent.revoked",
        target=employee_id,
        ip=ip,
        details=f"revoked_count={count}",
    )
    return count

def _safe_unlink(p: Path) -> bool:

    try:
        p = p.resolve()
        p.relative_to(config.DATA_DIR.resolve())
    except Exception:
        LOGGER.warning("Ignorando path fora de DATA_DIR: %s", p)
        return False
    try:
        p.unlink(missing_ok=True)
        return True
    except OSError as exc:
        LOGGER.warning("Falha ao remover %s: %s", p, exc)
        return False

def apply_retention_policy() -> dict[str, int]:
    """Aplica politicas de retencao configuradas. Retorna contadores."""
    result = {
        "punch_images_removed": 0,
        "orphan_pending_removed": 0,
        "audit_rows_removed": 0,
    }

    if config.DATA_RETENTION_DAYS > 0:
        cutoff = datetime.now(timezone.utc) - timedelta(days=config.DATA_RETENTION_DAYS)
        cutoff_iso = cutoff.isoformat(timespec="seconds")

        for path_str in db.collect_punch_image_paths_before(cutoff_iso):
            if _safe_unlink(Path(path_str)):
                result["punch_images_removed"] += 1

        for p in config.PUNCH_IMAGES_DIR.glob("*"):
            if not p.is_file():
                continue
            try:
                mtime = datetime.fromtimestamp(p.stat().st_mtime, tz=timezone.utc)
            except OSError:
                continue
            if mtime < cutoff and _safe_unlink(p):
                result["orphan_pending_removed"] += 1

    if config.AUDIT_LOG_RETENTION_DAYS > 0:
        cutoff = datetime.now(timezone.utc) - timedelta(days=config.AUDIT_LOG_RETENTION_DAYS)
        result["audit_rows_removed"] = db.delete_audit_before(cutoff.isoformat(timespec="seconds"))

    if any(v for v in result.values()):
        db.add_audit(
            actor="system",
            action="retention.applied",
            target=None,
            details=json.dumps(result),
        )
    return result

def export_employee_data(employee_id: str) -> dict[str, Any]:
    employee = db.get_employee(employee_id)
    if not employee:
        raise ValueError("Colaborador nao encontrado.")

    data = {
        "exported_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "consent_version_at_export": config.CONSENT_VERSION,
        "employee": employee,
        "consents": db.list_consents(employee_id),
        "punches": db.list_punches_for(employee_id),
        "voice_commands": db.list_voice_commands(employee_id=employee_id, limit=100000),
    }

    person_dir = config.DATASET_DIR / employee_id
    if person_dir.exists():
        data["dataset_images"] = sorted(
            p.name for p in person_dir.iterdir()
            if p.suffix.lower() in {".png", ".jpg", ".jpeg"}
        )
    else:
        data["dataset_images"] = []

    db.add_audit(actor="admin", action="lgpd.export", target=employee_id)
    return data

def erase_employee_data(employee_id: str, *, actor: str = "admin", ip: str | None = None) -> dict[str, int]:
    """Exerce direito ao esquecimento.

    - Remove amostras de dataset e imagens de ponto vinculadas.
    - Revoga consentimentos ativos.
    - Remove comandos de voz.
    - Anonimiza o cadastro do colaborador mantendo o historico de pontos
      (sem PII) para cumprimento de obrigacoes contabeis/legais.
    """
    result = {"dataset_files_removed": 0, "punch_images_removed": 0}

    employee = db.get_employee(employee_id)
    if not employee:
        raise ValueError("Colaborador nao encontrado.")

    for punch in db.list_punches_for(employee_id):
        image_path = punch.get("image_path")
        if image_path and _safe_unlink(Path(image_path)):
            result["punch_images_removed"] += 1

    person_dir = config.DATASET_DIR / employee_id
    if person_dir.exists() and person_dir.is_dir():
        for img in person_dir.iterdir():
            if img.is_file() and _safe_unlink(img):
                result["dataset_files_removed"] += 1
        try:
            person_dir.rmdir()
        except OSError:
            pass

    db.revoke_consents(employee_id)
    db.anonymize_employee(employee_id)

    db.add_audit(
        actor=actor,
        action="lgpd.erase",
        target=employee_id,
        ip=ip,
        details=json.dumps(result),
    )
    return result
