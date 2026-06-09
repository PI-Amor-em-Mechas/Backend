from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from pathlib import Path

from dicio_client import buscar_varias_palavras
from src.voice.training import load_phrases, save_phrases

ROOT = Path(__file__).resolve().parent
REPORT_PATH = ROOT / "data" / "dicio_voice_training_report.json"
RUNTIME_PATH = ROOT / "data" / "voice_phrases.json"
BASELINE_PATH = ROOT / "ptbr_vocab_cotidiano_v1.voice_phrases.json"

PONTO = "ponto"
ENTRADA = "entrada"
SAIDA = "saída"
REGISTRO = "registro"
HORARIO = "horário"
PRESENCA = "presença"
TRABALHO = "trabalho"
EXPEDIENTE = "expediente"
ATIVIDADE = "atividade"
PAUSA = "pausa"
INTERVALO = "intervalo"
DOACAO = "doação"
SERVICO = "serviço"
ATENDIMENTO = "atendimento"
TRIAGEM = "triagem"

SEED_WORDS = [
    PONTO,
    ENTRADA,
    SAIDA,
    REGISTRO,
    "registrar",
    "marcar",
    "confirmar",
    "cancelar",
    "corrigir",
    HORARIO,
    PRESENCA,
    TRABALHO,
    EXPEDIENTE,
    ATIVIDADE,
    PAUSA,
    INTERVALO,
    "voluntário",
    "voluntariado",
    DOACAO,
    "cabelo",
    "peruca",
    "paciente",
    ATENDIMENTO,
    TRIAGEM,
    "estoque",
    "kit",
]

ACTION_TEMPLATES = [
    "registrar {termo}",
    "marcar {termo}",
    "anotar {termo}",
    "confirmar {termo}",
    "corrigir {termo}",
]

DOMAIN_TEMPLATES = [
    "atividade de {termo}",
    "trabalho de {termo}",
    "registro de {termo}",
    "atendimento de {termo}",
]

STOPWORDS = {
    "a",
    "o",
    "as",
    "os",
    "de",
    "do",
    "da",
    "dos",
    "das",
    "em",
    "para",
    "por",
    "com",
    "sem",
}

ALLOWED_DICIO_TERMS = {
    ENTRADA: {"ingresso", "acesso"},
    SAIDA: {"partida"},
    REGISTRO: {"cadastro", "anotação"},
    HORARIO: {"hora", EXPEDIENTE},
    PRESENCA: {"comparecimento"},
    TRABALHO: {ATIVIDADE, SERVICO},
    ATIVIDADE: {"tarefa", SERVICO},
    PAUSA: {INTERVALO},
    INTERVALO: {PAUSA},
    DOACAO: {"donativo", "contribuição"},
    ATENDIMENTO: {"assistência"},
    TRIAGEM: {"seleção"},
}

ACTION_TERMS = {
    PONTO,
    ENTRADA,
    SAIDA,
    REGISTRO,
    "cadastro",
    "anotação",
    HORARIO,
    "hora",
    PRESENCA,
    "comparecimento",
    PAUSA,
    INTERVALO,
    EXPEDIENTE,
}

DOMAIN_TERMS = {
    TRABALHO,
    SERVICO,
    "tarefa",
    "voluntário",
    "voluntariado",
    DOACAO,
    "donativo",
    "contribuição",
    "cabelo",
    "peruca",
    "paciente",
    ATENDIMENTO,
    "assistência",
    TRIAGEM,
    "seleção",
    "estoque",
    "kit",
}

BARE_TERM_ALLOWED = ACTION_TERMS | DOMAIN_TERMS
COMMAND_WORDS = {"salvar", "confirmar", "cancelar", "corrigir", "registrar", "marcar"}
SELF_REFERENTIAL = {
    "registrar registro",
    "atividade de atividade",
    "trabalho de trabalho",
    "registro de registro",
    "atendimento de atendimento",
}


def _normalizar(texto: str) -> str:
    texto = " ".join((texto or "").strip().lower().split())
    return re.sub(r"[?!.]+$", "", texto)


def _termo_valido(texto: str) -> bool:
    termo = _normalizar(texto)
    if not termo or termo in STOPWORDS:
        return False
    if len(termo) < 3 or len(termo) > 38:
        return False
    if not re.search(r"[a-záàâãéêíóôõúç]", termo, flags=re.I):
        return False
    return not re.search(r"[{}\[\]<>$@#]", termo)


def _extrair_termos(verbete: dict, seed: str) -> list[str]:
    termos = []
    palavra = verbete.get("palavra")
    if isinstance(palavra, str) and _termo_valido(palavra):
        termos.append(palavra)

    permitidos = ALLOWED_DICIO_TERMS.get(seed, set())
    for sinonimo in verbete.get("sinonimos") or []:
        normalizado = _normalizar(sinonimo) if isinstance(sinonimo, str) else ""
        if normalizado in permitidos and _termo_valido(normalizado):
            termos.append(normalizado)

    vistos = set()
    unicos = []
    for termo in termos:
        normalizado = _normalizar(termo)
        if normalizado not in vistos:
            vistos.add(normalizado)
            unicos.append(normalizado)
    return unicos


def _gerar_frases(termos: list[str]) -> list[str]:
    frases = []
    for termo in termos:
        if termo in BARE_TERM_ALLOWED:
            frases.append(termo)

        templates = []
        if termo in ACTION_TERMS and termo not in COMMAND_WORDS:
            templates.extend(ACTION_TEMPLATES)
        if termo in DOMAIN_TERMS:
            templates.extend(DOMAIN_TEMPLATES)

        for template in templates:
            frase = template.format(termo=termo)
            if frase not in SELF_REFERENTIAL:
                frases.append(frase)
    return frases


def _carregar_baseline() -> list[str]:
    if not BASELINE_PATH.exists():
        return load_phrases()
    payload = json.loads(BASELINE_PATH.read_text(encoding="utf-8"))
    frases = payload.get("phrases") if isinstance(payload, dict) else payload
    if not isinstance(frases, list):
        return load_phrases()
    return [frase for frase in frases if isinstance(frase, str)]


def _salvar_backup_runtime() -> Path | None:
    if not RUNTIME_PATH.exists():
        return None
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup = RUNTIME_PATH.with_name(f"voice_phrases.before_dicio_training_{timestamp}.json")
    backup.write_text(RUNTIME_PATH.read_text(encoding="utf-8"), encoding="utf-8")
    return backup


def treinar_com_dicio() -> dict:
    antes = load_phrases()
    backup = _salvar_backup_runtime()
    baseline = _carregar_baseline()
    verbetes = buscar_varias_palavras(SEED_WORDS)

    termos = []
    for seed, verbete in zip(SEED_WORDS, verbetes):
        termos.extend(_extrair_termos(verbete, seed))

    candidatos = _gerar_frases(termos)
    adicionadas = []
    vistas = set(baseline)
    for frase in candidatos:
        normalizada = _normalizar(frase)
        if normalizada and normalizada not in vistas:
            vistas.add(normalizada)
            adicionadas.append(normalizada)

    save_phrases(baseline + adicionadas)
    depois = load_phrases()

    relatorio = {
        "status": "ok",
        "fonte": "Dicio",
        "gerado_em": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "palavras_consultadas": SEED_WORDS,
        "verbetes_encontrados": sum(1 for item in verbetes if item.get("significado")),
        "termos_extraidos": len(set(termos)),
        "candidatos_gerados": len(set(candidatos)),
        "frases_antes": len(antes),
        "baseline_usado": str(BASELINE_PATH.relative_to(ROOT)),
        "frases_baseline": len(baseline),
        "frases_depois": len(depois),
        "frases_adicionadas": len(adicionadas),
        "frases_removidas_do_treinamento_anterior": max(0, len(antes) - len(baseline)),
        "backup_runtime": str(backup.relative_to(ROOT)) if backup else None,
        "frases_adicionadas_lista": adicionadas,
    }
    REPORT_PATH.write_text(json.dumps(relatorio, ensure_ascii=False, indent=2), encoding="utf-8")
    return relatorio


if __name__ == "__main__":
    print(json.dumps(treinar_com_dicio(), ensure_ascii=False, indent=2))
