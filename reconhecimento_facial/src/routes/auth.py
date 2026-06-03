"""Blueprint de autenticacao / perfil e pagina inicial."""
from __future__ import annotations

from flask import Blueprint, jsonify, redirect, render_template, request, session, url_for

from .. import db
from ..security import (
    PROFILE_ADMIN,
    PROFILE_DEFAULT,
    check_admin_password,
    current_profile,
)

bp = Blueprint("auth", __name__)

@bp.get("/login")
def login_page():
    if current_profile() is not None:
        return redirect(url_for("auth.index"))
    return render_template("login.html")

@bp.get("/")
def index():
    if current_profile() is None:
        return redirect(url_for("auth.login_page"))
    return render_template("index.html")

@bp.get("/me")
def me():
    return jsonify({"status": "ok", "profile": current_profile()})

@bp.post("/set-profile")
def set_profile():
    payload = request.get_json(silent=True) or {}
    profile = str(payload.get("profile", "")).strip().lower()
    password = str(payload.get("password", ""))
    ip = request.remote_addr

    if profile == PROFILE_DEFAULT:
        session["profile"] = PROFILE_DEFAULT
        db.add_audit(actor="default", action="auth.login", ip=ip)
        return jsonify({"status": "ok", "profile": PROFILE_DEFAULT})

    if profile == PROFILE_ADMIN:
        if not check_admin_password(password):
            db.add_audit(actor="unknown", action="auth.login_failed", ip=ip)
            return jsonify({"status": "error", "message": "Senha admin invalida."}), 401
        session["profile"] = PROFILE_ADMIN
        db.add_audit(actor="admin", action="auth.login", ip=ip)
        return jsonify({"status": "ok", "profile": PROFILE_ADMIN})

    return jsonify({"status": "error", "message": "Perfil invalido."}), 400

@bp.post("/logout")
def logout():
    actor = current_profile() or "unknown"
    session.pop("profile", None)
    db.add_audit(actor=actor, action="auth.logout", ip=request.remote_addr)
    if request.is_json:
        return jsonify({"status": "ok"})
    return redirect(url_for("auth.login_page"))
