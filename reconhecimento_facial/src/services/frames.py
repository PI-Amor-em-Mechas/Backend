"""Utilitarios para tratamento de frames (decode/encode) e imagens de referencia."""
from __future__ import annotations

import base64
import secrets
from pathlib import Path

import cv2
import numpy as np

from .. import config

def decode_frame_from_data_uri(data_uri: str) -> np.ndarray:
    if not data_uri:
        raise ValueError("Imagem ausente para reconhecimento.")
    encoded = data_uri.split(",", 1)[1] if "," in data_uri else data_uri
    image_bytes = base64.b64decode(encoded)
    image_np = np.frombuffer(image_bytes, dtype=np.uint8)
    frame = cv2.imdecode(image_np, cv2.IMREAD_COLOR)
    if frame is None:
        raise ValueError("Nao foi possivel decodificar a imagem enviada.")
    return frame

def to_data_uri(face_bgr: np.ndarray) -> str:
    ok, buffer = cv2.imencode(".jpg", face_bgr)
    if not ok:
        raise RuntimeError("Falha ao codificar imagem de rosto.")
    b64 = base64.b64encode(buffer.tobytes()).decode("ascii")
    return f"data:image/jpeg;base64,{b64}"

def to_data_uri_from_file(image_path: Path) -> str | None:
    image = cv2.imread(str(image_path), cv2.IMREAD_UNCHANGED)
    if image is None:
        return None
    return to_data_uri(image)

def get_reference_image(employee_id: str, fallback_face: np.ndarray) -> str:
    person_dir = config.DATASET_DIR / employee_id
    if person_dir.exists() and person_dir.is_dir():
        sample_files = sorted(
            p
            for p in person_dir.iterdir()
            if p.suffix.lower() in {".png", ".jpg", ".jpeg"}
        )
        for sample in sample_files:
            data_uri = to_data_uri_from_file(sample)
            if data_uri:
                return data_uri
    return to_data_uri(fallback_face)

def save_pending_face(employee_id: str, face_bgr: np.ndarray) -> str | None:
    """Salva face temporariamente apenas se `SAVE_PUNCH_IMAGE` estiver ativo."""
    if not config.SAVE_PUNCH_IMAGE:
        return None
    token = secrets.token_hex(8)
    filename = f"pending_{employee_id}_{token}.png"
    path = config.PUNCH_IMAGES_DIR / filename
    ok = cv2.imwrite(str(path), face_bgr)
    return str(path) if ok else None
