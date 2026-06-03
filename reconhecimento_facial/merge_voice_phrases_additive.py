from __future__ import annotations

import json
import re
import subprocess
from datetime import datetime
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent
RUNTIME_PATH = ROOT / "data" / "voice_phrases.json"
BACKUP_PATH = ROOT / "data" / "voice_phrases.json.bak"
VOCAB_PATH = ROOT / "ptbr_vocab_cotidiano_v1.json"
IMPORTABLE_PATH = ROOT / "ptbr_vocab_cotidiano_v1.voice_phrases.json"

GIT_CANDIDATES = [
    "HEAD:reconhecimento_facial/data/voice_phrases.json",
    "HEAD:./data/voice_phrases.json",
    "HEAD:data/voice_phrases.json",
]

CONTROL_KEYS = {
    "intent",
    "utterance",
    "language",
    "schema",
    "source",
    "notes",
    "created_at",
    "count",
    "counts",
    "categories",
    "aliases",
    "words",
    "phrases",
}


def normalize(text: str) -> str:
    text = str(text or "").strip().lower()
    text = re.sub(r"\s+", " ", text)
    text = re.sub(r"[?!.]+$", "", text)
    return text


def add_unique(target: list[str], seen: set[str], value: str) -> None:
    phrase = normalize(value)
    if not phrase or phrase in seen:
        return
    if phrase in {"[", "]", "{", "}", ","} or phrase.startswith(("{", "[", "}", "]")):
        return
    if phrase.endswith(",") and '"' in phrase:
        return
    seen.add(phrase)
    target.append(phrase)


def add_string(target: list[str], seen: set[str], value: str) -> None:
    extracted = extract_from_loose_line(value)
    add_unique(target, seen, extracted or value)


def add_string_items(items: Any, target: list[str], seen: set[str]) -> None:
    if isinstance(items, list):
        for item in items:
            if isinstance(item, str):
                add_string(target, seen, item)


def add_categories(categories: Any, target: list[str], seen: set[str]) -> None:
    if not isinstance(categories, dict):
        return
    for items in categories.values():
        add_string_items(items, target, seen)


def add_aliases(aliases: Any, target: list[str], seen: set[str]) -> None:
    if not isinstance(aliases, dict):
        return
    for canonical, aliases_list in aliases.items():
        if isinstance(canonical, str):
            add_string(target, seen, canonical)
        add_string_items(aliases_list, target, seen)


def walk_json(value: Any, target: list[str], seen: set[str]) -> None:
    if isinstance(value, str):
        add_string(target, seen, value)
        return
    if isinstance(value, list):
        for item in value:
            walk_json(item, target, seen)
        return
    if not isinstance(value, dict):
        return

    utterance = value.get("utterance")
    if isinstance(utterance, str):
        add_string(target, seen, utterance)
    add_string_items(value.get("phrases"), target, seen)
    add_string_items(value.get("words"), target, seen)
    add_categories(value.get("categories"), target, seen)
    add_aliases(value.get("aliases"), target, seen)


def parse_json_text(text: str, target: list[str], seen: set[str]) -> bool:
    try:
        payload = json.loads(text)
    except Exception:
        return False
    walk_json(payload, target, seen)
    return True


def extract_from_loose_line(line: str) -> str | None:
    stripped = line.strip().strip(",")
    if not stripped:
        return None
    try:
        payload = json.loads(stripped)
    except Exception:
        payload = None
    if isinstance(payload, dict) and isinstance(payload.get("utterance"), str):
        return payload["utterance"]
    if isinstance(payload, str):
        return payload
    match = re.search(r'"utterance"\s*:\s*"([^"]+)"', stripped)
    if match:
        return match.group(1)
    match = re.match(r'^"(.+)"$', stripped)
    return match.group(1) if match else None


def parse_loose_text(text: str, target: list[str], seen: set[str]) -> None:
    for line in text.splitlines():
        value = extract_from_loose_line(line)
        if value:
            add_string(target, seen, value)


def load_file(path: Path, target: list[str], seen: set[str]) -> int:
    before = len(target)
    if not path.exists():
        return 0
    text = path.read_text(encoding="utf-8")
    if not parse_json_text(text, target, seen):
        parse_loose_text(text, target, seen)
    return len(target) - before


def load_git(target: list[str], seen: set[str]) -> int:
    before = len(target)
    for candidate in GIT_CANDIDATES:
        proc = subprocess.run(
            ["git", "show", candidate],
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        if proc.returncode == 0 and proc.stdout.strip():
            if not parse_json_text(proc.stdout, target, seen):
                parse_loose_text(proc.stdout, target, seen)
            break
    return len(target) - before


def main() -> None:
    phrases: list[str] = []
    seen: set[str] = set()
    source_counts = {
        "current_runtime": load_file(RUNTIME_PATH, phrases, seen),
        "backup": load_file(BACKUP_PATH, phrases, seen),
        "git_head": 0,
        "vocab": 0,
    }
    source_counts["git_head"] = load_git(phrases, seen)
    source_counts["vocab"] = load_file(VOCAB_PATH, phrases, seen)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    safety_backup = RUNTIME_PATH.with_name(f"voice_phrases.before_additive_merge_{timestamp}.json")
    if RUNTIME_PATH.exists():
        safety_backup.write_text(RUNTIME_PATH.read_text(encoding="utf-8"), encoding="utf-8")

    RUNTIME_PATH.write_text(
        json.dumps(phrases, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    importable_payload = {
        "language": "pt-BR",
        "schema": "voice_phrases_v1",
        "source": "additive_merge",
        "notes": "Mesclagem aditiva de frases existentes, backup, Git e vocabulário PT-BR. Formato compatível com /voice-phrases/import-json.",
        "phrases": phrases,
        "count": len(phrases),
        "source_counts": source_counts,
    }
    IMPORTABLE_PATH.write_text(
        json.dumps(importable_payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(json.dumps({
        "status": "ok",
        "runtime_path": str(RUNTIME_PATH.relative_to(ROOT)),
        "importable_path": str(IMPORTABLE_PATH.relative_to(ROOT)),
        "safety_backup": str(safety_backup.relative_to(ROOT)),
        "phrases": len(phrases),
        "source_counts": source_counts,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
