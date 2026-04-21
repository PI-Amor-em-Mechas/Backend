"""Blueprint de treino de frases para reconhecimento de voz (admin)."""
from __future__ import annotations

import io
import json
from datetime import datetime, timezone

from flask import Blueprint, jsonify, request, send_file

from ..security import require_admin_profile
from ..voice.training import add_phrase, add_phrases, load_phrases, remove_phrase

bp = Blueprint("voice_phrases", __name__, url_prefix="/voice-phrases")

@bp.get("")
@require_admin_profile
def list_voice_phrases():
    phrases = load_phrases()
    return jsonify({"status": "ok", "phrases": phrases, "count": len(phrases)})

@bp.post("/add")
@require_admin_profile
def add():
    payload = request.get_json(silent=True) or {}
    phrase = str(payload.get("phrase", "")).strip()
    if not phrase:
        return jsonify({"status": "error", "message": "Informe uma frase para treino."}), 400
    phrases = add_phrase(phrase)
    return jsonify({"status": "ok", "phrases": phrases, "count": len(phrases),
                    "message": "Frase adicionada ao treino de voz."})

@bp.post("/remove")
@require_admin_profile
def remove():
    payload = request.get_json(silent=True) or {}
    phrase = str(payload.get("phrase", "")).strip()
    if not phrase:
        return jsonify({"status": "error", "message": "Informe a frase para remover."}), 400
    phrases = remove_phrase(phrase)
    return jsonify({"status": "ok", "phrases": phrases, "count": len(phrases),
                    "message": "Frase removida do treino de voz."})

@bp.post("/bulk")
@require_admin_profile
def bulk():
    payload = request.get_json(silent=True) or {}
    raw_text = str(payload.get("text", ""))
    lines = [line.strip() for line in raw_text.splitlines() if line.strip()]
    if not lines:
        return jsonify({"status": "error", "message": "Informe ao menos uma frase por linha."}), 400
    phrases = add_phrases(lines)
    return jsonify({"status": "ok", "phrases": phrases, "count": len(phrases),
                    "message": "Treino de voz atualizado em lote."})

@bp.get("/export-json")
@require_admin_profile
def export_json():
    phrases = load_phrases()
    payload = json.dumps(
        {
            "phrases": phrases,
            "count": len(phrases),
            "exported_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        },
        ensure_ascii=False,
        indent=2,
    ).encode("utf-8")
    return send_file(
        io.BytesIO(payload),
        mimetype="application/json",
        as_attachment=True,
        download_name="voice_phrases.json",
    )

@bp.post("/import-json")
@require_admin_profile
def import_json():
    if "file" not in request.files:
        return jsonify({"status": "error", "message": "Arquivo JSON nao enviado."}), 400
    file = request.files.get("file")
    if file is None or not file.filename:
        return jsonify({"status": "error", "message": "Selecione um arquivo JSON."}), 400

    try:
        raw = file.read().decode("utf-8")
        data = json.loads(raw)
    except Exception:
        return jsonify({"status": "error", "message": "JSON invalido."}), 400

    texts: list[str]
    if isinstance(data, list):
        texts = [str(item) for item in data if isinstance(item, str)]
    elif isinstance(data, dict):
        raw_phrases = data.get("phrases")
        if not isinstance(raw_phrases, list):
            return jsonify({"status": "error", "message": "JSON deve conter chave 'phrases' como lista."}), 400
        texts = [str(item) for item in raw_phrases if isinstance(item, str)]
    else:
        return jsonify({"status": "error", "message": "Formato JSON nao suportado."}), 400

    if not texts:
        return jsonify({"status": "error", "message": "Nenhuma frase valida encontrada no JSON."}), 400

    phrases = add_phrases(texts)
    return jsonify({"status": "ok", "phrases": phrases, "count": len(phrases),
                    "message": "Frases importadas do JSON com sucesso."})
