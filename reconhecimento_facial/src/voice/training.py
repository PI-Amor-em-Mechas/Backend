"""Persistencia simples de frases para adaptar reconhecimento Vosk localmente."""

import json
from pathlib import Path

import reconhecimento_facial.src.config as config


def _normalize(text: str) -> str:
    return " ".join((text or "").strip().lower().split())


def load_phrases() -> list[str]:
    path: Path = config.VOICE_PHRASES_PATH
    if not path.exists():
        return []

    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return []

    if not isinstance(payload, list):
        return []

    phrases: list[str] = []
    for item in payload:
        if isinstance(item, str):
            normalized = _normalize(item)
            if normalized:
                phrases.append(normalized)
    if config.VOICE_MAX_PHRASES > 0:
        return phrases[: config.VOICE_MAX_PHRASES]
    return phrases


def save_phrases(phrases: list[str]) -> None:
    config.ensure_directories()
    unique: list[str] = []
    seen: set[str] = set()
    for p in phrases:
        n = _normalize(p)
        if n and n not in seen:
            seen.add(n)
            unique.append(n)
    if config.VOICE_MAX_PHRASES > 0:
        unique = unique[: config.VOICE_MAX_PHRASES]
    config.VOICE_PHRASES_PATH.write_text(
        json.dumps(unique, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def add_phrase(text: str) -> list[str]:
    phrase = _normalize(text)
    if not phrase:
        return load_phrases()

    current = load_phrases()
    if phrase not in current:
        current.append(phrase)
    save_phrases(current)
    return current


def add_phrases(texts: list[str]) -> list[str]:
    current = load_phrases()
    seen = set(current)
    for text in texts:
        phrase = _normalize(text)
        if not phrase or phrase in seen:
            continue
        current.append(phrase)
        seen.add(phrase)
    save_phrases(current)
    return current


def remove_phrase(text: str) -> list[str]:
    phrase = _normalize(text)
    if not phrase:
        return load_phrases()

    current = [p for p in load_phrases() if p != phrase]
    save_phrases(current)
    return current
