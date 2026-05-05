"""Testes smoke: cada endpoint HTTP eh chamado pelo menos uma vez.

Verifica que os handlers respondem sem erro 500 inesperado e que retornam
o codigo HTTP esperado para o caso minimo (sucesso ou validacao 4xx).
"""
from __future__ import annotations

import io
import json


# ---------------------------------------------------------------------------
# auth.py
# ---------------------------------------------------------------------------
def test_get_login(client):
    resp = client.get("/login")
    assert resp.status_code == 200


def test_get_index_redirects_when_anonymous(client):
    resp = client.get("/", follow_redirects=False)
    assert resp.status_code in (301, 302)
    assert "/login" in resp.headers.get("Location", "")


def test_get_index_when_authenticated(default_client):
    resp = default_client.get("/")
    assert resp.status_code == 200


def test_get_me(client):
    resp = client.get("/me")
    assert resp.status_code == 200
    body = resp.get_json()
    assert body["status"] == "ok"
    assert "profile" in body


def test_set_profile_default(client):
    resp = client.post("/set-profile", json={"profile": "default"})
    assert resp.status_code == 200
    assert resp.get_json()["profile"] == "default"


def test_set_profile_admin_invalid_password(client):
    resp = client.post("/set-profile", json={"profile": "admin", "password": "errada"})
    assert resp.status_code == 401


def test_set_profile_invalid(client):
    resp = client.post("/set-profile", json={"profile": "??"})
    assert resp.status_code == 400


def test_logout(default_client):
    resp = default_client.post("/logout", json={})
    assert resp.status_code == 200


# ---------------------------------------------------------------------------
# employees.py (admin)
# ---------------------------------------------------------------------------
def test_register_window_requires_admin(client):
    resp = client.get("/register-window", follow_redirects=False)
    # nao autenticado -> redirect para login
    assert resp.status_code in (302, 403)


def test_register_window_admin(admin_client):
    resp = admin_client.get("/register-window")
    assert resp.status_code == 200


def test_employees_list(admin_client):
    resp = admin_client.get("/employees")
    assert resp.status_code == 200
    assert resp.get_json()["status"] == "ok"


def test_employees_update_validation(admin_client):
    resp = admin_client.post("/employees/update", json={})
    assert resp.status_code == 400


def test_employees_update_ok(admin_client):
    resp = admin_client.post(
        "/employees/update",
        json={"employee_id": "e1", "employee_name": "Novo Nome"},
    )
    assert resp.status_code == 200
    assert resp.get_json()["status"] == "updated"


def test_employees_delete_validation(admin_client):
    resp = admin_client.post("/employees/delete", json={})
    assert resp.status_code == 400


def test_employees_delete_ok(admin_client):
    resp = admin_client.post("/employees/delete", json={"employee_id": "e1"})
    assert resp.status_code == 200
    assert resp.get_json()["status"] == "deleted"


def test_register_person_validation(admin_client):
    resp = admin_client.post("/register-person", json={})
    assert resp.status_code == 400


def test_register_person_requires_consent(admin_client):
    resp = admin_client.post(
        "/register-person",
        json={
            "employee_id": "e1",
            "employee_name": "Teste",
            "images": ["data:image/png;base64,xxxx"],
            "consent": False,
        },
    )
    assert resp.status_code == 400
    assert "Consentimento" in resp.get_json()["message"]


def test_train_model(admin_client):
    resp = admin_client.post("/train-model")
    assert resp.status_code == 200
    assert resp.get_json()["status"] == "trained"


# ---------------------------------------------------------------------------
# recognition.py
# ---------------------------------------------------------------------------
def test_recognition_window(default_client):
    resp = default_client.get("/recognition-window")
    assert resp.status_code == 200


def test_recognize_without_camera(default_client):
    # _capture_frame foi mockado para sempre falhar no ambiente de teste.
    resp = default_client.post("/recognize")
    assert resp.status_code == 500
    assert resp.get_json()["status"] == "error"


def test_recognize_frame_missing_image(default_client):
    resp = default_client.post("/recognize-frame", json={})
    assert resp.status_code == 400


def test_confirm_missing_token(default_client):
    resp = default_client.post("/confirm", json={})
    assert resp.status_code == 400


def test_confirm_expired_token(default_client):
    resp = default_client.post("/confirm", json={"token": "inexistente", "confirm": True})
    assert resp.status_code == 400
    assert resp.get_json()["status"] == "expired"


# ---------------------------------------------------------------------------
# lgpd.py
# ---------------------------------------------------------------------------
def test_lgpd_privacy_notice(client):
    resp = client.get("/lgpd/privacy-notice")
    assert resp.status_code == 200
    assert resp.get_json()["status"] == "ok"


def test_lgpd_grant_consent(admin_client):
    resp = admin_client.post("/lgpd/consent/e1")
    assert resp.status_code == 200


def test_lgpd_revoke_consent(admin_client):
    resp = admin_client.post("/lgpd/consent/e1/revoke")
    assert resp.status_code == 200


def test_lgpd_list_consent(admin_client):
    resp = admin_client.get("/lgpd/consent/e1")
    assert resp.status_code == 200


def test_lgpd_export(admin_client):
    resp = admin_client.get("/lgpd/export/e1")
    assert resp.status_code == 200
    payload = json.loads(resp.data.decode("utf-8"))
    assert payload["employee"]["id"] == "e1"


def test_lgpd_erase(admin_client):
    resp = admin_client.post("/lgpd/erase/e1")
    assert resp.status_code == 200


def test_lgpd_retention_apply(admin_client):
    resp = admin_client.post("/lgpd/retention/apply")
    assert resp.status_code == 200


def test_lgpd_audit(admin_client):
    resp = admin_client.get("/lgpd/audit?limit=10")
    assert resp.status_code == 200


# ---------------------------------------------------------------------------
# tts.py
# ---------------------------------------------------------------------------
def test_tts_speak_missing_text(default_client):
    resp = default_client.post("/tts/speak", json={})
    assert resp.status_code == 400


def test_tts_speak_ok(default_client):
    resp = default_client.post("/tts/speak", json={"text": "ola mundo"})
    assert resp.status_code == 200
    assert resp.mimetype == "audio/wav"


def test_tts_info(default_client):
    resp = default_client.get("/tts/info")
    assert resp.status_code == 200
    assert resp.get_json()["status"] == "ok"


# ---------------------------------------------------------------------------
# voice_phrases.py (admin)
# ---------------------------------------------------------------------------
def test_voice_phrases_list_empty(admin_client):
    resp = admin_client.get("/voice-phrases")
    assert resp.status_code == 200
    body = resp.get_json()
    assert body["status"] == "ok"
    assert body["count"] == 0


def test_voice_phrases_add_validation(admin_client):
    resp = admin_client.post("/voice-phrases/add", json={})
    assert resp.status_code == 400


def test_voice_phrases_add_ok(admin_client):
    resp = admin_client.post("/voice-phrases/add", json={"phrase": "iniciar atendimento"})
    assert resp.status_code == 200
    assert resp.get_json()["count"] >= 1


def test_voice_phrases_remove_validation(admin_client):
    resp = admin_client.post("/voice-phrases/remove", json={})
    assert resp.status_code == 400


def test_voice_phrases_remove_ok(admin_client):
    admin_client.post("/voice-phrases/add", json={"phrase": "remover essa"})
    resp = admin_client.post("/voice-phrases/remove", json={"phrase": "remover essa"})
    assert resp.status_code == 200


def test_voice_phrases_bulk_validation(admin_client):
    resp = admin_client.post("/voice-phrases/bulk", json={"text": "   "})
    assert resp.status_code == 400


def test_voice_phrases_bulk_ok(admin_client):
    resp = admin_client.post("/voice-phrases/bulk", json={"text": "frase um\nfrase dois"})
    assert resp.status_code == 200
    assert resp.get_json()["count"] >= 2


def test_voice_phrases_export_json(admin_client):
    resp = admin_client.get("/voice-phrases/export-json")
    assert resp.status_code == 200
    payload = json.loads(resp.data.decode("utf-8"))
    assert "phrases" in payload


def test_voice_phrases_import_json_missing_file(admin_client):
    resp = admin_client.post("/voice-phrases/import-json", data={})
    assert resp.status_code == 400


def test_voice_phrases_import_json_ok(admin_client):
    data = {
        "file": (io.BytesIO(json.dumps(["alpha", "beta"]).encode("utf-8")), "phrases.json"),
    }
    resp = admin_client.post(
        "/voice-phrases/import-json",
        data=data,
        content_type="multipart/form-data",
    )
    assert resp.status_code == 200
    assert resp.get_json()["count"] >= 2


# ---------------------------------------------------------------------------
# Autorizacao: chamadas admin sem login devem ser bloqueadas
# ---------------------------------------------------------------------------
def test_admin_endpoint_without_profile_is_forbidden(client):
    resp = client.get("/employees")
    assert resp.status_code == 403


def test_admin_endpoint_with_default_profile_is_forbidden(default_client):
    resp = default_client.get("/employees")
    assert resp.status_code == 403
