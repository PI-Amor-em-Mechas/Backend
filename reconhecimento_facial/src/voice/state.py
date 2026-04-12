"""Estado de sessao de voz por socket/usuario."""

from dataclasses import dataclass, field

from vosk import KaldiRecognizer


@dataclass
class VoiceSession:
    """Um estado isolado por conexao WebSocket."""
    user_id: str
    user_name: str
    recognizer: KaldiRecognizer
    accumulated_text: str = ""
    partial_text: str = ""
