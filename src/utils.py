import logging
import urllib.request
from datetime import datetime, timezone
from typing import Any

import cv2
import mediapipe as mp
import numpy as np
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision

import config

LOGGER = logging.getLogger(__name__)


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def ensure_face_detector_model() -> None:
    config.ensure_directories()
    model_path = config.FACE_DETECTOR_MODEL_PATH

    if model_path.exists() and model_path.stat().st_size > 0:
        return

    if model_path.exists() and model_path.stat().st_size == 0:
        model_path.unlink(missing_ok=True)

    errors: list[str] = []
    for url in config.FACE_DETECTOR_MODEL_URLS:
        LOGGER.info("Downloading MediaPipe face detector model from %s", url)
        try:
            with urllib.request.urlopen(url, timeout=30) as response:
                data = response.read()
            if not data:
                raise RuntimeError("Downloaded model file is empty")
            model_path.write_bytes(data)
            LOGGER.info("Face detector model saved to %s", model_path)
            return
        except Exception as exc:
            errors.append(f"- {url}: {exc}")

    reasons = "\n".join(errors)
    raise RuntimeError(
        "Could not download face detector model automatically. "
        "Download a compatible MediaPipe Face Detector model manually and place it at "
        "data/face_detector.task. Tried URLs:\n"
        f"{reasons}"
    )


def create_face_detector() -> Any:
    ensure_face_detector_model()
    options = vision.FaceDetectorOptions(
        base_options=mp_python.BaseOptions(
            model_asset_path=str(config.FACE_DETECTOR_MODEL_PATH)
        ),
        running_mode=vision.RunningMode.IMAGE,
        min_detection_confidence=config.MP_MIN_DETECTION_CONFIDENCE,
    )
    return vision.FaceDetector.create_from_options(options)


def _clip_bbox(x: int, y: int, w: int, h: int, width: int, height: int) -> tuple[int, int, int, int]:
    x = max(0, x)
    y = max(0, y)
    w = max(0, w)
    h = max(0, h)

    if x + w > width:
        w = width - x
    if y + h > height:
        h = height - y

    return x, y, max(0, w), max(0, h)


def detect_largest_face(
    frame_bgr: np.ndarray,
    detector: Any,
) -> dict[str, Any] | None:
    rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
    result = detector.detect(mp_image)

    if not result.detections:
        return None

    best_det = None
    best_area = -1.0
    for det in result.detections:
        bbox = det.bounding_box
        area = float(max(0, bbox.width) * max(0, bbox.height))
        if area > best_area:
            best_area = area
            best_det = det

    if best_det is None:
        return None

    ih, iw = frame_bgr.shape[:2]
    bbox = best_det.bounding_box
    x, y, w, h = _clip_bbox(
        int(bbox.origin_x),
        int(bbox.origin_y),
        int(bbox.width),
        int(bbox.height),
        iw,
        ih,
    )

    if w <= 0 or h <= 0:
        return None

    face_crop = frame_bgr[y : y + h, x : x + w]
    if face_crop.size == 0:
        return None

    keypoints = []
    for kp in best_det.keypoints:
        kx = int(kp.x * iw)
        ky = int(kp.y * ih)
        keypoints.append((kx, ky))

    return {
        "bbox": (x, y, w, h),
        "keypoints": keypoints,
        "face_crop": face_crop,
        "detection": best_det,
    }


def preprocess_face(face_bgr: np.ndarray) -> np.ndarray:
    gray = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2GRAY)
    gray = cv2.resize(gray, config.FACE_IMAGE_SIZE)
    if config.EQUALIZE_HIST:
        gray = cv2.equalizeHist(gray)
    return gray


def draw_overlay(
    frame: np.ndarray,
    bbox: tuple[int, int, int, int],
    keypoints: list[tuple[int, int]],
    label: str,
    confidence: float | None,
    known: bool,
) -> None:
    x, y, w, h = bbox
    color = (0, 180, 0) if known else (0, 0, 255)
    cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)

    for px, py in keypoints:
        cv2.circle(frame, (px, py), 2, (255, 200, 0), -1)

    conf_text = ""
    if confidence is not None:
        conf_text = f" ({confidence:.1f})"
    cv2.putText(
        frame,
        f"{label}{conf_text}",
        (x, max(20, y - 10)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.6,
        color,
        2,
        cv2.LINE_AA,
    )
