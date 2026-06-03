from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VOCAB_PATH = ROOT / "ptbr_vocab_cotidiano_v1.json"
RUNTIME_PATH = ROOT / "data" / "voice_phrases.json"
IMPORTABLE_PATH = ROOT / "ptbr_vocab_cotidiano_v1.voice_phrases.json"

CORE_COMMANDS = [
    "salvar",
    "apagar",
    "cancelar",
    "confirmar",
    "sim",
    "não",
    "pode salvar",
    "pode apagar",
    "pode cancelar",
    "pode confirmar",
    "confirmar registro",
    "cancelar registro",
    "registrar entrada",
    "registrar saída",
    "registrar minha entrada",
    "registrar minha saída",
    "registrar meu ponto",
    "bater o ponto",
    "marcar presença",
    "dar entrada",
    "dar baixa",
    "fechar o ponto",
    "iniciar pausa",
    "encerrar pausa",
    "voltei da pausa",
    "retornar do intervalo",
]

DOMAIN_PHRASE_HINTS = (
    "ponto",
    "entrada",
    "saída",
    "saida",
    "horário",
    "horario",
    "registro",
    "registrar",
    "madrinha",
    "voluntariado",
    "voluntário",
    "voluntaria",
    "ong",
    "kit",
    "kits",
    "amor",
    "mechas",
    "peruca",
    "perucas",
    "paciente",
    "pacientes",
    "doação",
    "doações",
    "cabelo",
    "atividade",
    "atividades",
    "pausa",
    "intervalo",
    "almoço",
    "almoco",
    "horas",
    "histórico",
    "historico",
    "relatório",
    "relatorio",
    "corrigir",
    "erro",
    "duplicado",
)

NOISY_PATTERNS = (
    "paula paula",
    "lua fala",
    "número",
    "numero",
)


def normalize(text: str) -> str:
    text = str(text or "").strip().lower()
    text = re.sub(r"[?!.]+$", "", text)
    text = re.sub(r"\s+", " ", text)
    return text


def is_useful_phrase(text: str) -> bool:
    phrase = normalize(text)
    if not phrase:
        return False
    if any(pattern in phrase for pattern in NOISY_PATTERNS):
        return False
    words = phrase.split()
    if len(words) == 1:
        return phrase in {"salvar", "apagar", "cancelar", "confirmar", "sim", "não"}
    if len(words) > 14:
        return False
    return any(hint in phrase for hint in DOMAIN_PHRASE_HINTS) or phrase in CORE_COMMANDS


def unique(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        phrase = normalize(item)
        if phrase and phrase not in seen:
            seen.add(phrase)
            result.append(phrase)
    return result


def main() -> None:
    vocab = json.loads(VOCAB_PATH.read_text(encoding="utf-8"))
    existing = json.loads(RUNTIME_PATH.read_text(encoding="utf-8"))

    vocab_phrases = [p for p in vocab.get("phrases", []) if isinstance(p, str)]
    aliases = vocab.get("aliases", {})
    alias_phrases: list[str] = []
    if isinstance(aliases, dict):
        for canonical, values in aliases.items():
            if isinstance(canonical, str):
                alias_phrases.append(canonical)
            if isinstance(values, list):
                alias_phrases.extend(v for v in values if isinstance(v, str))

    curated = unique(
        CORE_COMMANDS
        + [p for p in existing if isinstance(p, str) and is_useful_phrase(p)]
        + [p for p in vocab_phrases if isinstance(p, str)]
        + alias_phrases
    )

    payload = {
        "language": vocab.get("language", "pt-BR"),
        "schema": "voice_phrases_v1",
        "source": VOCAB_PATH.name,
        "notes": "Frases fechadas para Vosk em modo gramática restrita. Use a chave phrases no importador /voice-phrases/import-json.",
        "phrases": curated,
        "count": len(curated),
    }

    IMPORTABLE_PATH.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    RUNTIME_PATH.write_text(
        json.dumps(curated, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(json.dumps({
        "runtime_path": str(RUNTIME_PATH.relative_to(ROOT)),
        "importable_path": str(IMPORTABLE_PATH.relative_to(ROOT)),
        "phrases": len(curated),
        "grammar_mode_expected": True,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
