"""Vosk offline speech-to-text engine.

Carrega o modelo uma unica vez e cria um KaldiRecognizer por conexao.
"""

import json
import logging

from vosk import Model, KaldiRecognizer

import reconhecimento_facial.src.config as config

LOGGER = logging.getLogger(__name__)

_model: Model | None = None


def get_model() -> Model:
    """Retorna o modelo Vosk singleton (carrega na primeira chamada)."""
    global _model
    if _model is None:
        model_path = str(config.VOSK_MODEL_PATH)
        LOGGER.info("Carregando modelo Vosk de %s ...", model_path)
        _model = Model(model_path)
        LOGGER.info("Modelo Vosk carregado com sucesso.")
    return _model


def create_recognizer(
    sample_rate: int = 16000,
    phrases: list[str] | None = None,
) -> KaldiRecognizer:
    """Cria um novo KaldiRecognizer vinculado ao modelo global.

    Se `phrases` for informado, aplica gramatica local para melhorar comandos de dominio.
    """
    if phrases:
        grammar = [p for p in phrases if p]
        if "salvar" not in grammar:
            grammar.append("salvar")
        return KaldiRecognizer(get_model(), sample_rate, json.dumps(grammar, ensure_ascii=False))

    return KaldiRecognizer(get_model(), sample_rate)


def feed_audio(recognizer: KaldiRecognizer, data: bytes) -> tuple[str | None, str | None]:
    """Alimenta o recognizer com PCM16-LE mono.

    Retorna (final_text, partial_text):
      - final_text preenchido quando AcceptWaveform == True  (frase concluida)
      - partial_text preenchido caso contrario               (parcial)
    """
    if recognizer.AcceptWaveform(data):
        result = json.loads(recognizer.Result())
        return result.get("text", ""), None
    else:
        partial = json.loads(recognizer.PartialResult())
        return None, partial.get("partial", "")


def finalize(recognizer: KaldiRecognizer) -> str:
    """Encerra o stream e retorna qualquer texto restante no buffer."""
    result = json.loads(recognizer.FinalResult())
    return result.get("text", "")
