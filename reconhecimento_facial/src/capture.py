import logging

import cv2

import reconhecimento_facial.src.config as config
import reconhecimento_facial.src.db as db
from reconhecimento_facial.src.utils import create_face_detector, detect_largest_face, preprocess_face

LOGGER = logging.getLogger(__name__)


def capture_employee_samples(
    employee_id: str,
    employee_name: str,
    target_samples: int | None = None,
) -> int:
    target = int(target_samples or config.CAPTURE_TARGET_SAMPLES)
    if target <= 0:
        raise ValueError("target_samples must be greater than zero")

    cap = cv2.VideoCapture(config.CAMERA_INDEX)
    if not cap.isOpened():
        raise RuntimeError("Could not open webcam. Check camera index and permissions.")

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, config.FRAME_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, config.FRAME_HEIGHT)

    detector = create_face_detector()

    db.add_employee(employee_id, employee_name)

    person_dir = config.DATASET_DIR / employee_id
    person_dir.mkdir(parents=True, exist_ok=True)

    LOGGER.info(
        "Starting capture for employee id=%s name=%s target_samples=%d",
        employee_id,
        employee_name,
        target,
    )

    saved = 0
    frame_idx = 0
    last_face = None

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
                x, y, w, h = last_face["bbox"]
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 180, 0), 2)
                cv2.putText(
                    frame,
                    f"{employee_name} [{saved}/{target}]",
                    (x, max(20, y - 8)),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 180, 0),
                    2,
                    cv2.LINE_AA,
                )

                if frame_idx % config.CAPTURE_EVERY_N_FRAMES == 0 and saved < target:
                    face_gray = preprocess_face(last_face["face_crop"])
                    image_path = person_dir / f"{employee_id}_{saved:04d}.png"
                    cv2.imwrite(str(image_path), face_gray)
                    saved += 1
                    LOGGER.debug("Saved sample: %s", image_path)

            cv2.putText(
                frame,
                "q: quit | Capturing largest detected face",
                (10, 25),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (255, 255, 255),
                2,
                cv2.LINE_AA,
            )

            cv2.imshow("Capture Samples", frame)

            key = cv2.waitKey(1) & 0xFF
            if key == ord("q"):
                LOGGER.info("Capture interrupted by user")
                break

            if saved >= target:
                LOGGER.info("Capture completed with %d samples", saved)
                break

    finally:
        if hasattr(detector, "close"):
            detector.close()
        cap.release()
        cv2.destroyAllWindows()

    return saved


def prompt_and_capture() -> None:
    try:
        employee_id = input("Employee ID: ").strip()
        employee_name = input("Employee Name: ").strip()
        target_raw = input(
            f"Target samples [{config.CAPTURE_TARGET_SAMPLES}]: "
        ).strip()
    except EOFError:
        print("Entrada encerrada durante o cadastro.")
        return
    except KeyboardInterrupt:
        print("\nCadastro interrompido pelo usuario.")
        return

    if not employee_id or not employee_name:
        print("ID and name are required.")
        return

    target = config.CAPTURE_TARGET_SAMPLES
    if target_raw:
        try:
            target = int(target_raw)
        except ValueError:
            print("Invalid number. Using default target.")

    saved = capture_employee_samples(employee_id, employee_name, target)
    print(f"Saved {saved} samples for {employee_name} ({employee_id}).")
