"""Captura de amostras de rosto via webcam (CLI), usando YuNet + SFace.

Salva:
- BGR 112x112 aligned crop em `data/dataset/<id>/<id>_NNNN.png` (permite re-embed).
- Embedding SFace direto na tabela `face_embeddings`.
"""
from __future__ import annotations

import logging

import cv2

from . import config
from . import db
from .services.face_engine import (
    compute_embedding,
    detect_largest_face,
    encode_embedding,
    invalidate_index,
)
from .services.lgpd import has_valid_consent, privacy_notice, record_consent

LOGGER = logging.getLogger(__name__)

def capture_employee_samples(
    employee_id: str,
    employee_name: str,
    target_samples: int | None = None,
) -> int:
    target = int(target_samples or config.CAPTURE_TARGET_SAMPLES)
    if target <= 0:
        raise ValueError("target_samples must be greater than zero")

    if not has_valid_consent(employee_id):
        raise RuntimeError(
            "Consentimento LGPD ausente ou desatualizado para este colaborador."
        )

    cap = cv2.VideoCapture(config.CAMERA_INDEX)
    if not cap.isOpened():
        raise RuntimeError("Could not open webcam. Check camera index and permissions.")
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, config.FRAME_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, config.FRAME_HEIGHT)

    db.add_employee(employee_id, employee_name)
    person_dir = config.DATASET_DIR / employee_id
    person_dir.mkdir(parents=True, exist_ok=True)

    saved = 0
    frame_idx = 0
    last_face = None

    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            frame_idx += 1

            if frame_idx % config.PROCESS_EVERY_N_FRAMES == 0:
                last_face = detect_largest_face(frame)

            if last_face is not None:
                x, y, w, h = last_face.bbox
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 180, 0), 2)
                cv2.putText(frame, f"{employee_name} [{saved}/{target}]",
                            (x, max(20, y - 8)), cv2.FONT_HERSHEY_SIMPLEX,
                            0.7, (0, 180, 0), 2, cv2.LINE_AA)

                if frame_idx % config.CAPTURE_EVERY_N_FRAMES == 0 and saved < target:
                    image_path = person_dir / f"{employee_id}_{saved:04d}.png"
                    cv2.imwrite(str(image_path), last_face.aligned_crop)
                    vec = compute_embedding(last_face)
                    db.add_face_embedding(
                        employee_id=employee_id,
                        vec_bytes=encode_embedding(vec),
                        dim=vec.shape[0],
                        dtype="float32",
                    )
                    saved += 1

            cv2.putText(frame, "q: quit", (10, 25), cv2.FONT_HERSHEY_SIMPLEX,
                        0.6, (255, 255, 255), 2, cv2.LINE_AA)
            cv2.imshow("Capture Samples", frame)
            key = cv2.waitKey(1) & 0xFF
            if key == ord("q"):
                break
            if saved >= target:
                break
    finally:
        cap.release()
        cv2.destroyAllWindows()

    invalidate_index()
    return saved

def prompt_and_capture() -> None:
    try:
        employee_id = input("Employee ID: ").strip()
        employee_name = input("Employee Name: ").strip()
    except (EOFError, KeyboardInterrupt):
        return
    if not employee_id or not employee_name:
        print("ID and name are required.")
        return

    print("\n--- AVISO DE PRIVACIDADE (LGPD) ---")
    print(privacy_notice()["text"])
    try:
        consent = input("Confirma o consentimento? [s/N]: ").strip().lower()
    except (EOFError, KeyboardInterrupt):
        consent = ""
    if consent != "s":
        print("Consentimento nao concedido. Cadastro cancelado.")
        return

    db.add_employee(employee_id, employee_name)
    record_consent(employee_id, actor="cli")

    try:
        target_raw = input(
            f"Target samples [{config.CAPTURE_TARGET_SAMPLES}]: "
        ).strip()
    except (EOFError, KeyboardInterrupt):
        target_raw = ""
    target = config.CAPTURE_TARGET_SAMPLES
    if target_raw:
        try:
            target = int(target_raw)
        except ValueError:
            pass

    saved = capture_employee_samples(employee_id, employee_name, target)
    print(f"Saved {saved} samples for {employee_name} ({employee_id}).")
