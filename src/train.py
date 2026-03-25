import json
import logging

import cv2
import numpy as np

import config
import db

LOGGER = logging.getLogger(__name__)


def _build_lbph_recognizer():
    if not hasattr(cv2, "face"):
        raise RuntimeError(
            "cv2.face is not available. Install opencv-contrib-python and restart environment."
        )

    if hasattr(cv2.face, "LBPHFaceRecognizer_create"):
        return cv2.face.LBPHFaceRecognizer_create()

    if hasattr(cv2.face, "LBPHFaceRecognizer") and hasattr(cv2.face.LBPHFaceRecognizer, "create"):
        return cv2.face.LBPHFaceRecognizer.create()

    raise RuntimeError("LBPH Face Recognizer API not found in OpenCV build")


def train_model() -> None:
    config.ensure_directories()

    images: list[np.ndarray] = []
    labels: list[int] = []

    label_to_employee_id: dict[str, str] = {}
    employee_id_to_name = {
        e["id"]: e["name"]
        for e in db.list_employees()
    }

    label_counter = 0

    for person_dir in sorted(config.DATASET_DIR.iterdir()):
        if not person_dir.is_dir():
            continue

        employee_id = person_dir.name
        sample_files = sorted(
            [
                p
                for p in person_dir.iterdir()
                if p.suffix.lower() in {".png", ".jpg", ".jpeg"}
            ]
        )

        if not sample_files:
            LOGGER.warning("No samples found for employee_id=%s", employee_id)
            continue

        label_to_employee_id[str(label_counter)] = employee_id

        sample_count = 0
        for image_path in sample_files:
            img = cv2.imread(str(image_path), cv2.IMREAD_GRAYSCALE)
            if img is None:
                LOGGER.warning("Could not read image: %s", image_path)
                continue
            img = cv2.resize(img, config.FACE_IMAGE_SIZE)
            images.append(img)
            labels.append(label_counter)
            sample_count += 1

        LOGGER.info(
            "Loaded %d samples for employee_id=%s label=%d",
            sample_count,
            employee_id,
            label_counter,
        )
        label_counter += 1

    if not images:
        raise RuntimeError(
            "No training samples found. Capture samples first in data/dataset/<employee_id>."
        )

    recognizer = _build_lbph_recognizer()
    recognizer.train(images, np.array(labels, dtype=np.int32))
    recognizer.write(str(config.LBPH_MODEL_PATH))

    labels_payload = {
        "label_to_employee_id": label_to_employee_id,
        "employee_id_to_name": employee_id_to_name,
    }
    with config.LABELS_PATH.open("w", encoding="utf-8") as f:
        json.dump(labels_payload, f, indent=2, ensure_ascii=False)

    LOGGER.info("LBPH model saved to: %s", config.LBPH_MODEL_PATH)
    LOGGER.info("Labels map saved to: %s", config.LABELS_PATH)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    db.init_db()
    train_model()
