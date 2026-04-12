from pathlib import Path
import os

ROOT_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT_DIR / "data"
DATASET_DIR = DATA_DIR / "dataset"
MODELS_DIR = DATA_DIR / "models"
PUNCH_IMAGES_DIR = DATA_DIR / "punch_images"

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

CAMERA_INDEX = 0
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
SAVE_PUNCH_IMAGE = True

LOG_LEVEL = "INFO"
ADMIN_PROFILE_PASSWORD = "admin123"


def ensure_directories() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    DATASET_DIR.mkdir(parents=True, exist_ok=True)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    PUNCH_IMAGES_DIR.mkdir(parents=True, exist_ok=True)
