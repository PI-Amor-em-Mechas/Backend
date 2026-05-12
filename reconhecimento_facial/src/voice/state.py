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
    # Buffer PCM16-LE mono @ VOICE_SAMPLE_RATE acumulado para biometria de voz.
    # E reiniciado a cada save/clear/force_save e limitado por
    # config.VOICE_BIOMETRY_MAX_BUFFER_SECONDS para nao crescer indefinidamente.
    pcm_buffer: bytearray = field(default_factory=bytearray)
