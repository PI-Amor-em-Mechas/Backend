"""Biometria de voz (text-independent) com Resemblyzer / GE2E.

Usado como 2o fator de autenticacao apos o reconhecimento facial:
  1. Face confirma a identidade (YuNet + SFace) -> emite voice_token.
  2. Sessao de voz acumula PCM16-LE @ 16 kHz em paralelo ao Vosk.
  3. Antes de persistir o comando_voz, comparamos o embedding da fala com
     o centroid de voiceprints cadastrados do colaborador (cosine similarity).

O encoder e carregado de forma preguicosa para nao impactar o boot caso a
biometria de voz esteja desabilitada ou o torch nao esteja instalado ainda.
"""
from __future__ import annotations

import logging
import threading
from dataclasses import dataclass
from typing import Any

import numpy as np

from .. import config
from .. import db

LOGGER = logging.getLogger(__name__)

VOICE_EMBEDDING_DIM = 256
VOICE_EMBEDDING_DTYPE = "float32"
VOICE_TARGET_SAMPLE_RATE = 16000

_encoder_lock = threading.Lock()
_encoder: Any = None


@dataclass
class VoiceMatch:
    """Resultado de uma verificacao de voz contra o centroid de um colaborador."""
    matched: bool
    score: float
    threshold: float
    enrolled_samples: int
    reason: str = ""


def _get_encoder() -> Any:
    """Carrega o VoiceEncoder do Resemblyzer uma unica vez (lazy)."""
    global _encoder
    if _encoder is not None:
        return _encoder
    with _encoder_lock:
        if _encoder is None:
            try:
                from resemblyzer import VoiceEncoder  # type: ignore
            except Exception as exc:  # pragma: no cover - dep opcional
                raise RuntimeError(
                    "Resemblyzer nao instalado. Execute "
                    "`pip install resemblyzer torch librosa`."
                ) from exc
            LOGGER.info("Carregando VoiceEncoder (Resemblyzer)...")
            _encoder = VoiceEncoder(verbose=False)
            LOGGER.info("VoiceEncoder pronto.")
    return _encoder


def pcm16_to_float32(pcm_bytes: bytes) -> np.ndarray:
    """Converte PCM16-LE mono em float32 normalizado [-1, 1]."""
    if not pcm_bytes:
        return np.zeros(0, dtype=np.float32)
    arr = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32)
    arr /= 32768.0
    return arr


def encode_embedding(vec: np.ndarray) -> bytes:
    """Serializa um embedding float32 para gravacao em LONGBLOB."""
    return np.ascontiguousarray(vec, dtype=np.float32).tobytes()


def decode_embedding(blob: bytes, dim: int = VOICE_EMBEDDING_DIM) -> np.ndarray:
    arr = np.frombuffer(blob, dtype=np.float32)
    if arr.size != dim:
        raise ValueError(
            f"Voice embedding size mismatch: got {arr.size}, expected {dim}"
        )
    return arr


def _l2_normalize(vec: np.ndarray) -> np.ndarray:
    norm = float(np.linalg.norm(vec))
    if norm < 1e-12:
        return vec
    return vec / norm


def compute_embedding(samples: np.ndarray) -> np.ndarray:
    """Calcula o d-vector (256-D) para um waveform float32 mono @ 16 kHz."""
    if samples.dtype != np.float32:
        samples = samples.astype(np.float32)
    encoder = _get_encoder()
    emb = encoder.embed_utterance(samples)
    emb = np.asarray(emb, dtype=np.float32).reshape(-1)
    return _l2_normalize(emb)


def compute_embedding_from_pcm16(pcm_bytes: bytes) -> np.ndarray:
    return compute_embedding(pcm16_to_float32(pcm_bytes))


def get_centroid(employee_id: str) -> tuple[np.ndarray | None, int]:
    """Retorna (centroid normalizado, n_amostras) do colaborador, ou (None, 0)."""
    rows = db.list_voice_embeddings(employee_id)
    if not rows:
        return None, 0
    vectors: list[np.ndarray] = []
    for row in rows:
        try:
            v = decode_embedding(row["vec"], int(row["dim"]))
        except Exception as exc:
            LOGGER.warning(
                "Voice embedding invalida ignorada id=%s: %s", row.get("id"), exc
            )
            continue
        vectors.append(v)
    if not vectors:
        return None, 0
    matrix = np.stack(vectors, axis=0).astype(np.float32)
    centroid = matrix.mean(axis=0)
    return _l2_normalize(centroid), len(vectors)


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine de dois vetores ja normalizados (ou a normalizar)."""
    if a.size == 0 or b.size == 0:
        return 0.0
    a = _l2_normalize(a)
    b = _l2_normalize(b)
    return float(np.dot(a, b))


def verify_speaker(
    employee_id: str,
    pcm_bytes: bytes,
    *,
    threshold: float | None = None,
    sample_rate: int = VOICE_TARGET_SAMPLE_RATE,
) -> VoiceMatch:
    """Compara o audio acumulado contra o voiceprint do colaborador.

    Politica de aceitacao:
      - Se nao houver voiceprint e VOICE_BIOMETRY_ALLOW_UNENROLLED=True,
        considera matched (com reason="not_enrolled").
      - Caso contrario, calcula o embedding e compara via cosseno.
    """
    th = threshold if threshold is not None else config.VOICE_BIOMETRY_THRESHOLD

    centroid, n_samples = get_centroid(employee_id)
    if centroid is None:
        if config.VOICE_BIOMETRY_ALLOW_UNENROLLED:
            return VoiceMatch(
                matched=True, score=0.0, threshold=th,
                enrolled_samples=0, reason="not_enrolled",
            )
        return VoiceMatch(
            matched=False, score=0.0, threshold=th,
            enrolled_samples=0, reason="not_enrolled",
        )

    min_bytes = int(
        config.VOICE_BIOMETRY_MIN_SECONDS * sample_rate * 2  # 2 bytes/sample (PCM16)
    )
    if len(pcm_bytes) < min_bytes:
        return VoiceMatch(
            matched=False, score=0.0, threshold=th,
            enrolled_samples=n_samples, reason="too_short",
        )

    try:
        emb = compute_embedding_from_pcm16(pcm_bytes)
    except Exception as exc:
        LOGGER.exception("Falha ao calcular embedding de voz: %s", exc)
        return VoiceMatch(
            matched=False, score=0.0, threshold=th,
            enrolled_samples=n_samples, reason=f"encode_error:{exc}",
        )

    score = cosine_similarity(emb, centroid)
    return VoiceMatch(
        matched=score >= th,
        score=score,
        threshold=th,
        enrolled_samples=n_samples,
        reason="ok" if score >= th else "below_threshold",
    )


def enroll_sample(employee_id: str, pcm_bytes: bytes) -> dict[str, Any]:
    """Calcula e persiste um voiceprint a partir de um trecho PCM16-LE @ 16 kHz."""
    samples = pcm16_to_float32(pcm_bytes)
    duration = samples.size / float(VOICE_TARGET_SAMPLE_RATE)
    min_seconds = max(2.0, config.VOICE_BIOMETRY_MIN_SECONDS)
    if duration < min_seconds:
        raise ValueError(
            f"Amostra muito curta ({duration:.2f}s); minimo {min_seconds:.1f}s."
        )

    emb = compute_embedding(samples)
    emb_id = db.add_voice_embedding(
        employee_id=employee_id,
        vec_bytes=encode_embedding(emb),
        dim=int(emb.shape[0]),
        dtype=VOICE_EMBEDDING_DTYPE,
    )
    return {
        "id": emb_id,
        "duration_seconds": round(duration, 2),
        "dim": int(emb.shape[0]),
    }
