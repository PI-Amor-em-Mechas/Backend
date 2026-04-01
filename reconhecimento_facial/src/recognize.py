import json
import logging
from datetime import datetime, timezone

import cv2

import reconhecimento_facial.src.config as config
import reconhecimento_facial.src.db as db
from reconhecimento_facial.src.utils import create_face_detector, detect_largest_face, draw_overlay, preprocess_face, utc_now_iso

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


def _load_model_and_labels():
    if not config.LBPH_MODEL_PATH.exists() or not config.LABELS_PATH.exists():
        raise RuntimeError("Model files not found. Run training first.")

    recognizer = _build_lbph_recognizer()
    recognizer.read(str(config.LBPH_MODEL_PATH))

    with config.LABELS_PATH.open("r", encoding="utf-8") as f:
        labels = json.load(f)

    return recognizer, labels


def _parse_ts(ts_text: str) -> datetime:
    dt = datetime.fromisoformat(ts_text)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def _is_within_cooldown(employee_id: str) -> bool:
    last = db.get_last_punch(employee_id)
    if not last:
        return False

    now = datetime.now(timezone.utc)
    last_dt = _parse_ts(last["ts"])
    delta = (now - last_dt).total_seconds()
    return delta < config.PUNCH_DUPLICATE_WINDOW_SECONDS


def _next_punch_type(employee_id: str, forced: str | None = None) -> str:
    if forced in {"IN", "OUT"}:
        return forced

    last = db.get_last_punch(employee_id)
    if not last:
        return "IN"
    return "OUT" if last["type"] == "IN" else "IN"


def _save_punch_image(employee_id: str, face_bgr) -> str | None:
    if not config.SAVE_PUNCH_IMAGE:
        return None

    filename = f"{employee_id}_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S_%f')}.png"
    path = config.PUNCH_IMAGES_DIR / filename
    ok = cv2.imwrite(str(path), face_bgr)
    return str(path) if ok else None


def run_recognition_loop() -> None:
    recognizer, labels = _load_model_and_labels()
    label_to_employee_id: dict[str, str] = labels.get("label_to_employee_id", {})
    employee_id_to_name: dict[str, str] = labels.get("employee_id_to_name", {})

    cap = cv2.VideoCapture(config.CAMERA_INDEX)
    if not cap.isOpened():
        raise RuntimeError("Could not open webcam. Check camera index and permissions.")

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, config.FRAME_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, config.FRAME_HEIGHT)

    detector = create_face_detector()
    frame_idx = 0
    last_face = None
    forced_type = None
    status_text = ""
    status_ttl = 0

    LOGGER.info("Starting recognition loop")

    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                LOGGER.warning("Could not read frame from webcam")
                break

            frame_idx += 1
            if frame_idx % config.PROCESS_EVERY_N_FRAMES == 0:
                last_face = detect_largest_face(frame, detector)

            if last_face:
                face_gray = preprocess_face(last_face["face_crop"])
                pred_label, pred_confidence = recognizer.predict(face_gray)

                employee_id = label_to_employee_id.get(str(pred_label))
                known = bool(
                    employee_id and pred_confidence <= config.LBPH_CONFIDENCE_THRESHOLD
                )

                if known:
                    name = employee_id_to_name.get(employee_id, employee_id)
                    if not _is_within_cooldown(employee_id):
                        punch_type = _next_punch_type(employee_id, forced=forced_type)
                        forced_type = None

                        image_path = _save_punch_image(employee_id, last_face["face_crop"])
                        punch_id = db.add_punch(
                            employee_id=employee_id,
                            punch_type=punch_type,
                            confidence=float(pred_confidence),
                            image_path=image_path,
                            ts=utc_now_iso(),
                        )
                        status_text = (
                            f"PUNCH {punch_type}: {name} ({employee_id}) "
                            f"conf={pred_confidence:.1f} id={punch_id}"
                        )
                        status_ttl = 45
                        LOGGER.info(status_text)
                    else:
                        status_text = (
                            f"Cooldown active for {name} ({employee_id}), "
                            "punch not recorded"
                        )
                        status_ttl = 20
                else:
                    name = "Desconhecido"

                draw_overlay(
                    frame=frame,
                    bbox=last_face["bbox"],
                    keypoints=last_face["keypoints"],
                    label=name,
                    confidence=float(pred_confidence),
                    known=known,
                )
            else:
                cv2.putText(
                    frame,
                    "Nenhuma face detectada",
                    (10, 55),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (0, 180, 255),
                    2,
                    cv2.LINE_AA,
                )

            cv2.putText(
                frame,
                "q: sair | i: forcar IN | o: forcar OUT | auto alterna IN/OUT",
                (10, 25),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.55,
                (255, 255, 255),
                2,
                cv2.LINE_AA,
            )

            cv2.putText(
                frame,
                f"Threshold LBPH: <= {config.LBPH_CONFIDENCE_THRESHOLD:.1f} = conhecido",
                (10, frame.shape[0] - 15),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.52,
                (255, 255, 255),
                1,
                cv2.LINE_AA,
            )

            if status_ttl > 0 and status_text:
                cv2.putText(
                    frame,
                    status_text,
                    (10, frame.shape[0] - 40),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.56,
                    (80, 255, 80),
                    2,
                    cv2.LINE_AA,
                )
                status_ttl -= 1

            cv2.imshow("Face Recognition - Attendance", frame)
            key = cv2.waitKey(1) & 0xFF

            if key == ord("q"):
                break
            if key == ord("i"):
                forced_type = "IN"
                status_text = "Proximo registro forcado para IN"
                status_ttl = 30
            if key == ord("o"):
                forced_type = "OUT"
                status_text = "Proximo registro forcado para OUT"
                status_ttl = 30

    finally:
        if hasattr(detector, "close"):
            detector.close()
        cap.release()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    db.init_db()
    run_recognition_loop()
