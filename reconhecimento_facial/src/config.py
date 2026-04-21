"""Configuracoes centrais do Face Attendance.

Segredos e parametros sensiveis podem ser sobrescritos por variaveis de
ambiente. Nao versione `.env` ou o arquivo `data/secret_key`.
"""
from __future__ import annotations

import os
import secrets
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT_DIR / "data"
DATASET_DIR = DATA_DIR / "dataset"
MODELS_DIR = DATA_DIR / "models"
PUNCH_IMAGES_DIR = DATA_DIR / "punch_images"
EXPORTS_DIR = DATA_DIR / "exports"

DB_PATH = DATA_DIR / "attendance.db"
LBPH_MODEL_PATH = MODELS_DIR / "lbph_model.yml"
LABELS_PATH = MODELS_DIR / "labels.json"
VOSK_MODEL_PATH = DATA_DIR / "vosk-model"
VOICE_PHRASES_PATH = DATA_DIR / "voice_phrases.json"
VOICE_SAMPLE_RATE = 16000
VOICE_MAX_PHRASES = int(os.getenv("VOICE_MAX_PHRASES", "0"))

FACE_DETECTOR_MODEL_PATH = DATA_DIR / "face_detector.task"
FACE_DETECTOR_MODEL_URLS = [
    (
        "https://storage.googleapis.com/mediapipe-models/face_detector/"
        "blaze_face_short_range/float16/latest/blaze_face_short_range.tflite"
    ),
    (
        "https://storage.googleapis.com/mediapipe-models/face_detector/"
        "blaze_face_short_range/float16/1/blaze_face_short_range.tflite"
    ),
]

YUNET_MODEL_PATH = MODELS_DIR / "face_detection_yunet_2023mar.onnx"
YUNET_MODEL_URLS = [
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_detection_yunet/face_detection_yunet_2023mar.onnx",
]
SFACE_MODEL_PATH = MODELS_DIR / "face_recognition_sface_2021dec.onnx"
SFACE_MODEL_URLS = [
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_recognition_sface/face_recognition_sface_2021dec.onnx",
]

SFACE_DISTANCE_TYPE = "cosine"
SFACE_COSINE_THRESHOLD = float(os.getenv("SFACE_COSINE_THRESHOLD", "0.363"))
SFACE_L2_THRESHOLD = float(os.getenv("SFACE_L2_THRESHOLD", "1.128"))

YUNET_SCORE_THRESHOLD = float(os.getenv("YUNET_SCORE_THRESHOLD", "0.6"))
YUNET_NMS_THRESHOLD = float(os.getenv("YUNET_NMS_THRESHOLD", "0.3"))
YUNET_TOP_K = int(os.getenv("YUNET_TOP_K", "50"))

CAMERA_INDEX = int(os.getenv("CAMERA_INDEX", "0"))
FRAME_WIDTH = 960
FRAME_HEIGHT = 540
PROCESS_EVERY_N_FRAMES = 2

CAPTURE_TARGET_SAMPLES = 50
CAPTURE_EVERY_N_FRAMES = 3

FACE_IMAGE_SIZE = (200, 200)
EQUALIZE_HIST = True

MP_MIN_DETECTION_CONFIDENCE = 0.5
LBPH_CONFIDENCE_THRESHOLD = 65.0

PUNCH_DUPLICATE_WINDOW_SECONDS = 60

SAVE_PUNCH_IMAGE = os.getenv("SAVE_PUNCH_IMAGE", "false").lower() in {"1", "true", "yes"}

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

ADMIN_PROFILE_PASSWORD = os.getenv("ADMIN_PROFILE_PASSWORD", "")

SECRET_KEY_PATH = DATA_DIR / "secret_key"

def load_or_create_secret_key() -> bytes:
    env_value = os.getenv("FLASK_SECRET_KEY")
    if env_value:
        return env_value.encode("utf-8")

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if SECRET_KEY_PATH.exists():
        data = SECRET_KEY_PATH.read_bytes()
        if data:
            return data

    key = secrets.token_bytes(48)
    SECRET_KEY_PATH.write_bytes(key)
    try:
        os.chmod(SECRET_KEY_PATH, 0o600)
    except OSError:
        pass
    return key

CONSENT_VERSION = os.getenv("CONSENT_VERSION", "1.0")

DATA_RETENTION_DAYS = int(os.getenv("DATA_RETENTION_DAYS", "90"))

AUDIT_LOG_RETENTION_DAYS = int(os.getenv("AUDIT_LOG_RETENTION_DAYS", "365"))

ANONYMIZED_PUNCHES_RETENTION_DAYS = int(
    os.getenv("ANONYMIZED_PUNCHES_RETENTION_DAYS", "1825")
)

LOCAL_TIMEZONE = os.getenv("LOCAL_TIMEZONE", "America/Sao_Paulo")

def ensure_directories() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    DATASET_DIR.mkdir(parents=True, exist_ok=True)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    PUNCH_IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    EXPORTS_DIR.mkdir(parents=True, exist_ok=True)
