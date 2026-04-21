"""Pipeline de deteccao e reconhecimento facial com OpenCV (YuNet + SFace).

- Deteccao: `cv2.FaceDetectorYN` — ONNX pequeno, CPU-friendly, retorna bbox +
  5 landmarks + score no layout esperado pelo SFace.
- Reconhecimento: `cv2.FaceRecognizerSF` — extrai embedding 128-D a partir do
  rosto alinhado. Matching por similaridade de cosseno (threshold 0.363).

Modelos ONNX sao baixados automaticamente na primeira execucao.

Referencias:
- https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet
- https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface
"""
from __future__ import annotations

import logging
import threading
import urllib.request
from dataclasses import dataclass
from typing import Any

import cv2
import numpy as np

from .. import config
from .. import db

LOGGER = logging.getLogger(__name__)

_detector_lock = threading.Lock()
_recognizer_lock = threading.Lock()
_index_lock = threading.Lock()

_detector: Any = None
_detector_size: tuple[int, int] | None = None
_recognizer: Any = None

_emb_matrix: np.ndarray | None = None
_emb_employee_ids: list[str] = []
_emb_norms: np.ndarray | None = None
_emb_dirty = True

EMBEDDING_DIM = 128
EMBEDDING_DTYPE = "float32"

def _download_model(urls: list[str], dest) -> None:
    if dest.exists() and dest.stat().st_size > 0:
        return
    if dest.exists():
        dest.unlink(missing_ok=True)

    errors: list[str] = []
    for url in urls:
        LOGGER.info("Baixando modelo de %s", url)
        try:
            with urllib.request.urlopen(url, timeout=60) as resp:
                data = resp.read()
            if not data:
                raise RuntimeError("Arquivo vazio")
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
            LOGGER.info("Modelo salvo em %s", dest)
            return
        except Exception as exc:
            errors.append(f"- {url}: {exc}")

    raise RuntimeError(
        "Falha ao baixar modelo. Tentativas:\n" + "\n".join(errors)
    )

def ensure_models() -> None:
    config.ensure_directories()
    _download_model(config.YUNET_MODEL_URLS, config.YUNET_MODEL_PATH)
    _download_model(config.SFACE_MODEL_URLS, config.SFACE_MODEL_PATH)

def _get_detector(input_size: tuple[int, int]) -> Any:
    global _detector, _detector_size
    ensure_models()
    with _detector_lock:
        if _detector is None:
            _detector = cv2.FaceDetectorYN_create(
                str(config.YUNET_MODEL_PATH),
                "",
                input_size,
                config.YUNET_SCORE_THRESHOLD,
                config.YUNET_NMS_THRESHOLD,
                config.YUNET_TOP_K,
            )
            _detector_size = input_size
        elif _detector_size != input_size:
            _detector.setInputSize(input_size)
            _detector_size = input_size
        return _detector

def _get_recognizer() -> Any:
    global _recognizer
    ensure_models()
    with _recognizer_lock:
        if _recognizer is None:
            _recognizer = cv2.FaceRecognizerSF_create(
                str(config.SFACE_MODEL_PATH),
                "",
            )
        return _recognizer

@dataclass
class DetectedFace:
    """Resultado de uma deteccao: bbox XYWH, score, row raw e crop alinhado."""
    bbox: tuple[int, int, int, int]
    keypoints: list[tuple[int, int]]
    score: float
    raw_row: np.ndarray
    aligned_crop: np.ndarray

def _row_to_detected(row: np.ndarray, aligned: np.ndarray) -> DetectedFace:
    x = int(max(0, row[0]))
    y = int(max(0, row[1]))
    w = int(max(0, row[2]))
    h = int(max(0, row[3]))
    kps = []
    for i in range(5):
        kx = int(row[4 + i * 2])
        ky = int(row[5 + i * 2])
        kps.append((kx, ky))
    score = float(row[14])
    return DetectedFace(bbox=(x, y, w, h), keypoints=kps, score=score,
                        raw_row=row.astype(np.float32), aligned_crop=aligned)

def detect_faces(frame_bgr: np.ndarray) -> list[DetectedFace]:
    if frame_bgr is None or frame_bgr.size == 0:
        return []
    h, w = frame_bgr.shape[:2]
    detector = _get_detector((w, h))
    _, results = detector.detect(frame_bgr)
    if results is None:
        return []
    recognizer = _get_recognizer()
    faces: list[DetectedFace] = []
    for row in results:
        aligned = recognizer.alignCrop(frame_bgr, row)
        faces.append(_row_to_detected(row, aligned))
    return faces

def detect_largest_face(frame_bgr: np.ndarray) -> DetectedFace | None:
    faces = detect_faces(frame_bgr)
    if not faces:
        return None
    return max(faces, key=lambda f: f.bbox[2] * f.bbox[3])

def compute_embedding(face: DetectedFace) -> np.ndarray:
    recognizer = _get_recognizer()
    vec = recognizer.feature(face.aligned_crop)
    vec = np.asarray(vec, dtype=np.float32).reshape(-1)
    return vec

def encode_embedding(vec: np.ndarray) -> bytes:
    return np.ascontiguousarray(vec, dtype=np.float32).tobytes()

def decode_embedding(blob: bytes, dim: int = EMBEDDING_DIM) -> np.ndarray:
    arr = np.frombuffer(blob, dtype=np.float32)
    if arr.size != dim:
        raise ValueError(f"Embedding size mismatch: got {arr.size}, expected {dim}")
    return arr

def invalidate_index() -> None:
    global _emb_dirty
    with _index_lock:
        _emb_dirty = True

def _load_index() -> None:
    global _emb_matrix, _emb_employee_ids, _emb_norms, _emb_dirty
    rows = db.list_face_embeddings()
    if not rows:
        _emb_matrix = None
        _emb_employee_ids = []
        _emb_norms = None
        _emb_dirty = False
        return
    vectors: list[np.ndarray] = []
    ids: list[str] = []
    for row in rows:
        try:
            v = decode_embedding(row["vec"], int(row["dim"]))
        except Exception as exc:
            LOGGER.warning("Embedding invalida ignorada id=%s: %s", row.get("id"), exc)
            continue
        vectors.append(v)
        ids.append(row["employee_id"])
    if not vectors:
        _emb_matrix = None
        _emb_employee_ids = []
        _emb_norms = None
    else:
        matrix = np.stack(vectors, axis=0).astype(np.float32)
        norms = np.linalg.norm(matrix, axis=1)
        norms[norms == 0] = 1.0
        _emb_matrix = matrix
        _emb_employee_ids = ids
        _emb_norms = norms
    _emb_dirty = False

def _ensure_index() -> None:
    with _index_lock:
        if _emb_dirty:
            _load_index()

@dataclass
class MatchResult:
    employee_id: str | None
    score: float
    threshold: float
    known: bool

def match_embedding(vec: np.ndarray) -> MatchResult:
    _ensure_index()
    threshold = config.SFACE_COSINE_THRESHOLD
    if _emb_matrix is None or _emb_norms is None or not _emb_employee_ids:
        return MatchResult(employee_id=None, score=-1.0, threshold=threshold, known=False)

    vec_norm = float(np.linalg.norm(vec))
    if vec_norm == 0:
        return MatchResult(employee_id=None, score=-1.0, threshold=threshold, known=False)

    sims = (_emb_matrix @ vec) / (_emb_norms * vec_norm)

    best_by_emp: dict[str, float] = {}
    for emp_id, s in zip(_emb_employee_ids, sims, strict=False):
        prev = best_by_emp.get(emp_id)
        if prev is None or s > prev:
            best_by_emp[emp_id] = float(s)

    winner = max(best_by_emp.items(), key=lambda kv: kv[1])
    employee_id, score = winner
    return MatchResult(
        employee_id=employee_id,
        score=score,
        threshold=threshold,
        known=score >= threshold,
    )

def register_face_from_frame(employee_id: str, frame_bgr: np.ndarray) -> DetectedFace | None:
    """Detecta, computa embedding e persiste. Retorna o DetectedFace ou None."""
    face = detect_largest_face(frame_bgr)
    if face is None:
        return None
    vec = compute_embedding(face)
    db.add_face_embedding(
        employee_id=employee_id,
        vec_bytes=encode_embedding(vec),
        dim=vec.shape[0],
        dtype=EMBEDDING_DTYPE,
    )
    invalidate_index()
    return face

def rebuild_index_from_dataset() -> dict[str, int]:
    """Recomputa todas as embeddings a partir de `data/dataset/<id>/*.png`.

    Substitui o conceito de "treino" do LBPH. Util quando o modelo SFace muda
    ou quando se quer reprocessar as amostras existentes.
    """
    config.ensure_directories()
    ensure_models()

    db.clear_face_embeddings()
    stats: dict[str, int] = {}

    for person_dir in sorted(config.DATASET_DIR.iterdir()):
        if not person_dir.is_dir():
            continue
        employee_id = person_dir.name
        count = 0
        for img_path in sorted(person_dir.iterdir()):
            if img_path.suffix.lower() not in {".png", ".jpg", ".jpeg"}:
                continue
            img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
            if img is None:
                continue
            face = detect_largest_face(img)
            if face is None:
                LOGGER.warning("Nenhuma face detectada em %s", img_path)
                continue
            vec = compute_embedding(face)
            db.add_face_embedding(
                employee_id=employee_id,
                vec_bytes=encode_embedding(vec),
                dim=vec.shape[0],
                dtype=EMBEDDING_DTYPE,
            )
            count += 1
        stats[employee_id] = count
        LOGGER.info("Rebuild: %s -> %d embeddings", employee_id, count)

    invalidate_index()
    return stats

def draw_overlay(
    frame: np.ndarray,
    face: DetectedFace,
    label: str,
    score: float | None,
    known: bool,
) -> None:
    x, y, w, h = face.bbox
    color = (0, 180, 0) if known else (0, 0, 255)
    cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)
    for px, py in face.keypoints:
        cv2.circle(frame, (px, py), 2, (255, 200, 0), -1)
    score_text = ""
    if score is not None:
        score_text = f" ({score:.2f})"
    cv2.putText(
        frame,
        f"{label}{score_text}",
        (x, max(20, y - 10)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.6,
        color,
        2,
        cv2.LINE_AA,
    )
