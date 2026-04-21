"""Loop de reconhecimento desktop (OpenCV) usando YuNet + SFace."""
from __future__ import annotations

import logging
from datetime import datetime, timezone

import cv2

from . import config
from . import db
from .services.face_engine import (
    compute_embedding,
    detect_largest_face,
    draw_overlay,
    match_embedding,
)
from .services.punch_rules import determine_punch_type, is_within_cooldown
from .utils import utc_now_iso

LOGGER = logging.getLogger(__name__)

def _save_punch_image(employee_id: str, face_bgr) -> str | None:
    if not config.SAVE_PUNCH_IMAGE:
        return None
    filename = f"{employee_id}_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S_%f')}.png"
    path = config.PUNCH_IMAGES_DIR / filename
    ok = cv2.imwrite(str(path), face_bgr)
    return str(path) if ok else None

def _process_frame(frame, frame_idx, state):
    if frame_idx % config.PROCESS_EVERY_N_FRAMES == 0:
        state["last_face"] = detect_largest_face(frame)

    face = state["last_face"]
    if face is None:
        cv2.putText(frame, "Nenhuma face detectada", (10, 55),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 180, 255), 2, cv2.LINE_AA)
        return

    vec = compute_embedding(face)
    match = match_embedding(vec)

    name = "Desconhecido"
    if match.known and match.employee_id:
        emp = db.get_employee(match.employee_id) or {}
        name = emp.get("name") or match.employee_id
        if not is_within_cooldown(match.employee_id):
            punch_type = determine_punch_type(match.employee_id)
            image_path = _save_punch_image(match.employee_id, face.aligned_crop)
            punch_id = db.add_punch(
                employee_id=match.employee_id,
                punch_type=punch_type,
                confidence=float(match.score),
                image_path=image_path,
                ts=utc_now_iso(),
            )
            state["status_text"] = (
                f"PUNCH {punch_type}: {name} ({match.employee_id}) "
                f"sim={match.score:.2f} id={punch_id}"
            )
            state["status_ttl"] = 45
            LOGGER.info(state["status_text"])
        else:
            state["status_text"] = f"Cooldown: {name}"
            state["status_ttl"] = 20

    draw_overlay(frame, face, name, match.score, match.known)

def run_recognition_loop() -> None:
    cap = cv2.VideoCapture(config.CAMERA_INDEX)
    if not cap.isOpened():
        raise RuntimeError("Could not open webcam. Check camera index and permissions.")
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, config.FRAME_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, config.FRAME_HEIGHT)

    state = {"last_face": None, "status_text": "", "status_ttl": 0}
    frame_idx = 0

    LOGGER.info("Starting recognition loop (YuNet + SFace)")
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            frame_idx += 1
            _process_frame(frame, frame_idx, state)

            cv2.putText(frame, "q: sair | IN/OUT automatico por contagem diaria",
                        (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 2, cv2.LINE_AA)
            cv2.putText(frame,
                        f"SFace cosine >= {config.SFACE_COSINE_THRESHOLD:.2f} = conhecido",
                        (10, frame.shape[0] - 15), cv2.FONT_HERSHEY_SIMPLEX, 0.52,
                        (255, 255, 255), 1, cv2.LINE_AA)

            if state["status_ttl"] > 0 and state["status_text"]:
                cv2.putText(frame, state["status_text"], (10, frame.shape[0] - 40),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.56, (80, 255, 80), 2, cv2.LINE_AA)
                state["status_ttl"] -= 1

            cv2.imshow("Face Recognition - Attendance", frame)
            if (cv2.waitKey(1) & 0xFF) == ord("q"):
                break
    finally:
        cap.release()
        cv2.destroyAllWindows()

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    db.init_db()
    run_recognition_loop()
