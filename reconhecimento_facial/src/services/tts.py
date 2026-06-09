"""Servico de Text-To-Speech.

Suporta duas engines selecionadas por configuracao (``config.TTS_ENGINE``):

- ``piper`` (recomendado): TTS neural offline. Requer um modelo .onnx e seu
  .onnx.json correspondente em ``data/piper/``. Vozes pt-BR comunitarias:
      https://github.com/rhasspy/piper/blob/master/VOICES.md
  Para baixar (exemplo):
      huggingface-cli download rhasspy/piper-voices \
          pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx \
          pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx.json \
          --local-dir data/piper
  Ou simplesmente coloque os dois arquivos da voz em ``data/piper/``.

- ``pyttsx3`` (fallback): usa SAPI5 no Windows / espeak-ng no Linux. Util
  durante o desenvolvimento se ainda nao houver modelo Piper.

Default ``auto``: usa Piper se houver modelo disponivel, senao cai no pyttsx3.
"""
from __future__ import annotations

import io
import json
import logging
import os
import subprocess
import sys
import tempfile
import threading
import wave
from pathlib import Path

from .. import config

LOGGER = logging.getLogger(__name__)

_TTS_LOCK = threading.Lock()

# Cache do PiperVoice (carregar o modelo e caro).
_PIPER_VOICE = None
_PIPER_VOICE_PATH: Path | None = None


# ---------------------------------------------------------------------------
# Selecao de modelo / engine
# ---------------------------------------------------------------------------
def _resolve_piper_model() -> Path | None:
    """Retorna o caminho do .onnx do Piper a usar, ou None se ausente."""
    configured = (config.PIPER_VOICE or "").strip()
    if configured:
        candidate = Path(configured)
        if not candidate.is_absolute():
            candidate = config.PIPER_MODELS_DIR / configured
        if candidate.suffix != ".onnx":
            candidate = candidate.with_suffix(".onnx")
        if candidate.exists():
            return candidate
        LOGGER.warning("PIPER_VOICE configurado mas nao encontrado: %s", candidate)

    if config.PIPER_MODELS_DIR.exists():
        models = sorted(config.PIPER_MODELS_DIR.glob("*.onnx"))
        if models:
            return models[0]
    return None


def _piper_available() -> bool:
    try:
        import piper.voice  # type: ignore # noqa: F401
    except Exception:
        return False
    return _resolve_piper_model() is not None


def _select_engine() -> str:
    engine = (config.TTS_ENGINE or "auto").lower()
    if engine == "piper":
        return "piper"
    if engine == "pyttsx3":
        return "pyttsx3"
    return "piper" if _piper_available() else "pyttsx3"


# ---------------------------------------------------------------------------
# Backend Piper
# ---------------------------------------------------------------------------
def _load_piper_voice():
    global _PIPER_VOICE, _PIPER_VOICE_PATH

    model_path = _resolve_piper_model()
    if model_path is None:
        raise RuntimeError(
            f"Nenhum modelo Piper encontrado em {config.PIPER_MODELS_DIR}. "
            "Baixe um .onnx + .onnx.json (ex.: pt_BR-faber-medium)."
        )

    if _PIPER_VOICE is not None and _PIPER_VOICE_PATH == model_path:
        return _PIPER_VOICE

    config_path = model_path.with_suffix(model_path.suffix + ".json")
    if not config_path.exists():
        # Alguns pacotes nomeiam .json (sem o .onnx duplicado).
        alt = model_path.with_suffix(".json")
        config_path = alt if alt.exists() else config_path

    if not config_path.exists():
        raise RuntimeError(
            f"Configuracao do modelo Piper ausente: {config_path}"
        )

    from piper.voice import PiperVoice  # type: ignore

    LOGGER.info("Carregando modelo Piper: %s", model_path.name)
    _PIPER_VOICE = PiperVoice.load(str(model_path), config_path=str(config_path))
    _PIPER_VOICE_PATH = model_path
    return _PIPER_VOICE


def _piper_synthesize(text: str, length_scale: float | None = None) -> bytes:
    voice = _load_piper_voice()

    sample_rate = getattr(getattr(voice, "config", None), "sample_rate", 22050)

    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)  # int16
        wav_file.setframerate(int(sample_rate))

        kwargs: dict = {}
        if length_scale is not None:
            kwargs["length_scale"] = float(length_scale)

        # Compatibilidade entre versoes do piper-tts:
        try:
            for chunk in voice.synthesize(text, **kwargs):  # type: ignore[arg-type]
                _write_audio_chunk(wav_file, chunk)
        except TypeError:
            # API antiga: voice.synthesize(text, wav_file)
            voice.synthesize(text, wav_file)  # type: ignore[arg-type]

    return buffer.getvalue()


def _write_audio_chunk(wav_file: wave.Wave_write, chunk) -> None:
    """Aceita varios formatos de retorno entre versoes do piper-tts."""
    if isinstance(chunk, (bytes, bytearray, memoryview)):
        wav_file.writeframes(bytes(chunk))
        return

    audio_int16 = getattr(chunk, "audio_int16_bytes", None)
    if audio_int16 is not None:
        wav_file.writeframes(audio_int16)
        return

    audio_float = getattr(chunk, "audio_float_array", None)
    if audio_float is not None:
        try:
            import numpy as np  # type: ignore

            wav_file.writeframes((audio_float * 32767).astype(np.int16).tobytes())
            return
        except Exception:
            pass

    raise RuntimeError("Formato de chunk Piper nao reconhecido.")


# ---------------------------------------------------------------------------
# Backend pyttsx3 (fallback de desenvolvimento)
# ---------------------------------------------------------------------------
_PYTTSX3_WORKER = r"""
import json, sys, pyttsx3

cfg = json.loads(sys.stdin.read())
text = cfg["text"]
out_path = cfg["out"]
rate = int(cfg.get("rate", 175))
volume = float(cfg.get("volume", 1.0))

engine = pyttsx3.init()

preferred = ("portuguese", "portugues", "brazil", "brasil", "pt-br", "pt_br", "pt")
try:
    for voice in engine.getProperty("voices") or []:
        ident = " ".join(
            str(getattr(voice, attr, "") or "").lower()
            for attr in ("id", "name", "languages")
        )
        if any(k in ident for k in preferred):
            engine.setProperty("voice", voice.id)
            break
except Exception:
    pass

engine.setProperty("rate", rate)
engine.setProperty("volume", max(0.0, min(volume, 1.0)))
engine.save_to_file(text, out_path)
engine.runAndWait()
try:
    engine.stop()
except Exception:
    pass
"""


def _pyttsx3_synthesize(text: str, rate: int, volume: float) -> bytes:
    fd, tmp_name = tempfile.mkstemp(prefix="tts_", suffix=".wav")
    os.close(fd)
    tmp_path = Path(tmp_name)
    try:
        tmp_path.unlink(missing_ok=True)
        payload = json.dumps(
            {"text": text, "out": str(tmp_path), "rate": rate, "volume": volume}
        )

        proc = subprocess.run(
            [sys.executable, "-c", _PYTTSX3_WORKER],
            input=payload,
            capture_output=True,
            text=True,
            timeout=30,
            check=False,
        )
        if proc.returncode != 0:
            raise RuntimeError(
                f"pyttsx3 falhou (rc={proc.returncode}): {proc.stderr.strip()}"
            )
        if not tmp_path.exists() or tmp_path.stat().st_size == 0:
            raise RuntimeError("pyttsx3 nao gerou audio (arquivo vazio).")
        return tmp_path.read_bytes()
    finally:
        try:
            tmp_path.unlink(missing_ok=True)
        except Exception:
            pass


# ---------------------------------------------------------------------------
# API publica
# ---------------------------------------------------------------------------
def synthesize_to_wav(
    text: str,
    rate: int = 175,
    volume: float = 1.0,
    length_scale: float | None = None,
) -> bytes:
    """Sintetiza ``text`` em PT-BR e retorna bytes WAV.

    ``rate`` / ``volume`` so se aplicam ao backend pyttsx3.
    ``length_scale`` (Piper) controla a velocidade da fala (1.0 = padrao,
    >1.0 mais devagar, <1.0 mais rapido).
    """
    text = (text or "").strip()
    if not text:
        raise ValueError("Texto vazio para sintetizar.")

    engine = _select_engine()
    with _TTS_LOCK:
        if engine == "piper":
            wav = _piper_synthesize(text, length_scale=length_scale)
            LOGGER.info(
                "TTS[piper] gerou %d bytes para texto de %d caracteres.",
                len(wav), len(text),
            )
            return wav

        wav = _pyttsx3_synthesize(text, rate=rate, volume=volume)
        LOGGER.info(
            "TTS[pyttsx3] gerou %d bytes para texto de %d caracteres.",
            len(wav), len(text),
        )
        return wav


def get_engine_info() -> dict:
    """Retorna o engine ativo e o modelo Piper em uso (se aplicavel)."""
    engine = _select_engine()
    info: dict = {"engine": engine}
    if engine == "piper":
        model_path = _resolve_piper_model()
        info["model"] = model_path.name if model_path else None
        info["models_dir"] = str(config.PIPER_MODELS_DIR)
    return info
