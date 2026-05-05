"""Configuracao de testes (smoke).

Mocka a camada de banco (`src.db`), a captura de webcam e o motor TTS para
permitir que os endpoints sejam exercitados sem dependencias externas
(MariaDB, camera, modelos Piper/Vosk etc.).
"""
from __future__ import annotations

import os
import sys
from datetime import datetime, timezone
from pathlib import Path

import pytest

# Garante que o pacote `src` seja importavel quando rodando pytest a partir
# da raiz do projeto.
ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

os.environ.setdefault("ADMIN_PROFILE_PASSWORD", "admin123")


# ---------------------------------------------------------------------------
# Stubs para a camada de banco
# ---------------------------------------------------------------------------
def _install_db_stubs(monkeypatch) -> None:
    from src import db as db_module

    employee = {
        "id": "e1",
        "name": "Teste",
        "created_at": "2024-01-01 00:00:00",
        "anonymized_at": None,
    }
    consent = {
        "id": 1,
        "employee_id": "e1",
        "version": "1.0",
        "granted_at": "2024-01-01 00:00:00",
        "revoked_at": None,
        "user_agent": "pytest",
        "ip": "127.0.0.1",
    }

    fakes = {
        "init_db": lambda: None,
        "add_employee": lambda *a, **k: None,
        "get_employee": lambda employee_id: dict(employee, id=employee_id),
        "list_employees": lambda include_anonymized=False: [dict(employee)],
        "delete_employee": lambda *a, **k: 1,
        "anonymize_employee": lambda *a, **k: True,
        "add_punch": lambda *a, **k: 1,
        "get_last_punch": lambda *a, **k: None,
        "list_punches": lambda *a, **k: [],
        "list_punches_for": lambda *a, **k: [],
        "count_punches_between": lambda *a, **k: 0,
        "collect_punch_image_paths_before": lambda *a, **k: [],
        "delete_punches_before": lambda *a, **k: 0,
        "add_voice_command": lambda *a, **k: 1,
        "list_voice_commands": lambda *a, **k: [],
        "add_consent": lambda *a, **k: 1,
        "revoke_consents": lambda *a, **k: 1,
        "latest_consent": lambda employee_id: dict(consent, employee_id=employee_id),
        "list_consents": lambda employee_id: [dict(consent, employee_id=employee_id)],
        "add_audit": lambda *a, **k: None,
        "list_audit": lambda *a, **k: [],
        "delete_audit_before": lambda *a, **k: 0,
        "add_face_embedding": lambda *a, **k: 1,
        "list_face_embeddings": lambda: [],
        "count_face_embeddings": lambda *a, **k: 0,
        "delete_face_embeddings": lambda *a, **k: 0,
        "clear_face_embeddings": lambda: 0,
    }
    for name, fn in fakes.items():
        monkeypatch.setattr(db_module, name, fn, raising=False)


# ---------------------------------------------------------------------------
# Stubs para servicos pesados (camera, face engine, TTS)
# ---------------------------------------------------------------------------
def _install_service_stubs(monkeypatch) -> None:
    from src.routes import recognition as recognition_routes
    from src.services import face_engine
    from src.services import tts as tts_service

    def _fake_capture():
        raise RuntimeError("camera indisponivel em ambiente de teste")

    monkeypatch.setattr(recognition_routes, "_capture_frame", _fake_capture)

    monkeypatch.setattr(
        face_engine,
        "rebuild_index_from_dataset",
        lambda: {"employees": 0, "embeddings": 0},
    )
    monkeypatch.setattr(face_engine, "invalidate_index", lambda: None)

    def _fake_synth(text, rate=175, volume=1.0, length_scale=None):
        import struct
        header = b"RIFF" + struct.pack("<I", 36) + b"WAVE"
        header += b"fmt " + struct.pack("<IHHIIHH", 16, 1, 1, 16000, 32000, 2, 16)
        header += b"data" + struct.pack("<I", 0)
        return header

    monkeypatch.setattr(tts_service, "synthesize_to_wav", _fake_synth)
    monkeypatch.setattr(
        tts_service,
        "get_engine_info",
        lambda: {"engine": "stub", "voice": "stub", "available": True},
    )


# ---------------------------------------------------------------------------
# Fixtures publicas
# ---------------------------------------------------------------------------
@pytest.fixture
def app(monkeypatch, tmp_path):
    """Aplicacao Flask isolada com DB e servicos mockados."""
    _install_db_stubs(monkeypatch)
    _install_service_stubs(monkeypatch)

    from src import config as cfg

    monkeypatch.setattr(cfg, "VOICE_PHRASES_PATH", tmp_path / "voice_phrases.json")
    monkeypatch.setattr(cfg, "DATASET_DIR", tmp_path / "dataset")
    monkeypatch.setattr(cfg, "PUNCH_IMAGES_DIR", tmp_path / "punch_images")
    (tmp_path / "dataset").mkdir(parents=True, exist_ok=True)
    (tmp_path / "punch_images").mkdir(parents=True, exist_ok=True)

    from src.web_app import create_app

    flask_app, _ = create_app()
    flask_app.config["TESTING"] = True
    yield flask_app


@pytest.fixture
def client(app):
    return app.test_client()


@pytest.fixture
def admin_client(client):
    """Client autenticado como admin."""
    resp = client.post("/set-profile", json={"profile": "admin", "password": "admin123"})
    assert resp.status_code == 200, resp.get_json()
    return client


@pytest.fixture
def default_client(client):
    """Client autenticado como perfil default."""
    resp = client.post("/set-profile", json={"profile": "default"})
    assert resp.status_code == 200, resp.get_json()
    return client