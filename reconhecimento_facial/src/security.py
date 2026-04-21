"""Decoradores e helpers de autenticacao/perfil.

Perfis suportados:
- `default`: operacao de ponto (reconhecer/confirmar).
- `admin`:   gestao de colaboradores, treino, configuracoes LGPD.

Comparacao de senha usa `secrets.compare_digest` para mitigar timing attacks.
Se `ADMIN_PROFILE_PASSWORD` estiver vazio, o login admin eh bloqueado.
"""
from __future__ import annotations

import secrets
from functools import wraps

from flask import jsonify, redirect, request, session, url_for

from . import config

PROFILE_ADMIN = "admin"
PROFILE_DEFAULT = "default"

def current_profile() -> str | None:
    profile = session.get("profile")
    if profile in {PROFILE_ADMIN, PROFILE_DEFAULT}:
        return str(profile)
    return None

def check_admin_password(password: str) -> bool:
    expected = config.ADMIN_PROFILE_PASSWORD or ""
    if not expected:
        return False
    return secrets.compare_digest(password.encode("utf-8"), expected.encode("utf-8"))

def _forbidden_json(message: str = "Acesso negado."):
    return jsonify({"status": "forbidden", "message": message}), 403

def _is_window_get() -> bool:
    return request.method == "GET" and request.path.endswith("-window")

def require_any_profile(view):
    @wraps(view)
    def wrapper(*args, **kwargs):
        if current_profile() is None:
            if _is_window_get():
                return redirect(url_for("auth.login_page"))
            return _forbidden_json("Selecione um perfil antes de continuar.")
        return view(*args, **kwargs)

    return wrapper

def require_admin_profile(view):
    @wraps(view)
    def wrapper(*args, **kwargs):
        profile = current_profile()
        if profile is None:
            if _is_window_get():
                return redirect(url_for("auth.login_page"))
            return _forbidden_json("Selecione um perfil antes de continuar.")
        if profile != PROFILE_ADMIN:
            if _is_window_get():
                return redirect(url_for("auth.login_page"))
            return _forbidden_json("Somente perfil admin pode executar esta acao.")
        return view(*args, **kwargs)

    return wrapper
