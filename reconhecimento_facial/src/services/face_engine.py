"""
Pipeline de detecção e reconhecimento facial com OpenCV (YuNet + SFace).

- Detecção: `cv2.FaceDetectorYN`
  Modelo ONNX pequeno e adequado para CPU. Retorna bounding box,
  cinco landmarks faciais e score no layout utilizado pelo SFace.

- Reconhecimento: `cv2.FaceRecognizerSF`
  Extrai embedding facial de 128 dimensões a partir do rosto alinhado.

- Matching:
  Comparação por similaridade de cosseno utilizando o threshold
  configurado em `config.SFACE_COSINE_THRESHOLD`.

Os modelos ONNX são baixados automaticamente na primeira execução.
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


# ---------------------------------------------------------------------------
# Locks
# ---------------------------------------------------------------------------

_detector_lock = threading.Lock()
_recognizer_lock = threading.Lock()
_index_lock = threading.Lock()


# ---------------------------------------------------------------------------
# Instâncias dos modelos
# ---------------------------------------------------------------------------

_detector: Any = None
_detector_size: tuple[int, int] | None = None
_recognizer: Any = None


# ---------------------------------------------------------------------------
# Índice de embeddings
# ---------------------------------------------------------------------------

_emb_matrix: np.ndarray | None = None
_emb_employee_ids: list[str] = []
_emb_norms: np.ndarray | None = None
_emb_dirty = True


# ---------------------------------------------------------------------------
# Configuração dos embeddings SFace
# ---------------------------------------------------------------------------

EMBEDDING_DIM = 128
EMBEDDING_DTYPE = "float32"


# ---------------------------------------------------------------------------
# Download e carregamento dos modelos
# ---------------------------------------------------------------------------

def _download_model(urls: list[str], dest: Any) -> None:
    """
    Baixa um modelo utilizando a primeira URL disponível.

    O download não é executado caso o arquivo já exista e possua
    conteúdo.
    """

    if dest.exists() and dest.stat().st_size > 0:
        return

    if dest.exists():
        dest.unlink(missing_ok=True)

    errors: list[str] = []

    for url in urls:
        LOGGER.info("Baixando modelo de %s", url)

        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                data = response.read()

            if not data:
                raise RuntimeError("Arquivo de modelo vazio")

            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)

            LOGGER.info(
                "Modelo salvo em %s, tamanho=%d bytes",
                dest,
                len(data),
            )
            return

        except Exception as exc:
            errors.append(f"- {url}: {exc}")
            LOGGER.warning(
                "Falha ao baixar modelo de %s: %s",
                url,
                exc,
            )

    raise RuntimeError(
        "Falha ao baixar modelo. Tentativas:\n"
        + "\n".join(errors)
    )


def ensure_models() -> None:
    """Garante que os modelos YuNet e SFace estejam disponíveis."""

    config.ensure_directories()

    _download_model(
        config.YUNET_MODEL_URLS,
        config.YUNET_MODEL_PATH,
    )

    _download_model(
        config.SFACE_MODEL_URLS,
        config.SFACE_MODEL_PATH,
    )


def _get_detector(input_size: tuple[int, int]) -> Any:
    """
    Retorna a instância compartilhada do detector YuNet.

    O tamanho de entrada é atualizado quando a resolução do frame muda.
    """

    global _detector
    global _detector_size

    ensure_models()

    with _detector_lock:
        if _detector is None:
            LOGGER.info(
                "Inicializando YuNet com input_size=%s",
                input_size,
            )

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
    """Retorna a instância compartilhada do reconhecedor SFace."""

    global _recognizer

    ensure_models()

    with _recognizer_lock:
        if _recognizer is None:
            LOGGER.info("Inicializando modelo SFace")

            _recognizer = cv2.FaceRecognizerSF_create(
                str(config.SFACE_MODEL_PATH),
                "",
            )

        return _recognizer


# ---------------------------------------------------------------------------
# Detecção facial
# ---------------------------------------------------------------------------

@dataclass
class DetectedFace:
    """
    Resultado de uma detecção facial.

    Attributes:
        bbox:
            Bounding box no formato x, y, largura e altura.

        keypoints:
            Cinco landmarks faciais.

        score:
            Confiança retornada pelo detector.

        raw_row:
            Linha original retornada pelo YuNet.

        aligned_crop:
            Recorte facial alinhado para processamento pelo SFace.
    """

    bbox: tuple[int, int, int, int]
    keypoints: list[tuple[int, int]]
    score: float
    raw_row: np.ndarray
    aligned_crop: np.ndarray


def _row_to_detected(
    row: np.ndarray,
    aligned: np.ndarray,
) -> DetectedFace:
    """Converte uma linha retornada pelo YuNet em `DetectedFace`."""

    x = int(max(0, row[0]))
    y = int(max(0, row[1]))
    width = int(max(0, row[2]))
    height = int(max(0, row[3]))

    keypoints: list[tuple[int, int]] = []

    for index in range(5):
        point_x = int(row[4 + index * 2])
        point_y = int(row[5 + index * 2])
        keypoints.append((point_x, point_y))

    score = float(row[14])

    return DetectedFace(
        bbox=(x, y, width, height),
        keypoints=keypoints,
        score=score,
        raw_row=np.asarray(row, dtype=np.float32),
        aligned_crop=aligned,
    )


def detect_faces(frame_bgr: np.ndarray) -> list[DetectedFace]:
    """Detecta e alinha todas as faces encontradas em um frame BGR."""

    if frame_bgr is None or frame_bgr.size == 0:
        return []

    if frame_bgr.ndim != 3:
        LOGGER.warning(
            "Frame facial inválido: shape=%s",
            getattr(frame_bgr, "shape", None),
        )
        return []

    height, width = frame_bgr.shape[:2]

    if width <= 0 or height <= 0:
        return []

    detector = _get_detector((width, height))

    try:
        _, results = detector.detect(frame_bgr)
    except cv2.error:
        LOGGER.exception(
            "OpenCV falhou durante a detecção facial. frame_shape=%s",
            frame_bgr.shape,
        )
        raise

    if results is None:
        return []

    recognizer = _get_recognizer()
    faces: list[DetectedFace] = []

    for row in results:
        try:
            aligned = recognizer.alignCrop(frame_bgr, row)

            if aligned is None or aligned.size == 0:
                LOGGER.warning(
                    "SFace retornou recorte alinhado vazio"
                )
                continue

            faces.append(
                _row_to_detected(
                    row=row,
                    aligned=aligned,
                )
            )

        except cv2.error as exc:
            LOGGER.warning(
                "Falha ao alinhar face detectada: %s",
                exc,
            )

    return faces


def detect_largest_face(
    frame_bgr: np.ndarray,
) -> DetectedFace | None:
    """Retorna a maior face detectada no frame."""

    faces = detect_faces(frame_bgr)

    if not faces:
        return None

    return max(
        faces,
        key=lambda face: face.bbox[2] * face.bbox[3],
    )


# ---------------------------------------------------------------------------
# Geração, serialização e leitura de embeddings
# ---------------------------------------------------------------------------

def compute_embedding(face: DetectedFace) -> np.ndarray:
    """
    Calcula o embedding SFace da face detectada.

    O embedding válido deve possuir exatamente 128 valores float32,
    todos finitos e com norma diferente de zero.
    """

    recognizer = _get_recognizer()

    vector = recognizer.feature(face.aligned_crop)

    if vector is None:
        raise ValueError(
            "SFace não retornou um embedding"
        )

    vector = np.asarray(
        vector,
        dtype=np.float32,
    ).reshape(-1)

    if vector.size != EMBEDDING_DIM:
        raise ValueError(
            "SFace retornou embedding com dimensão "
            f"{vector.size}; esperado={EMBEDDING_DIM}"
        )

    if not np.all(np.isfinite(vector)):
        raise ValueError(
            "SFace retornou embedding contendo NaN ou infinito"
        )

    vector_norm = float(np.linalg.norm(vector))

    if vector_norm == 0:
        raise ValueError(
            "SFace retornou embedding com norma zero"
        )

    return np.ascontiguousarray(
        vector,
        dtype=np.float32,
    )


def encode_embedding(vector: np.ndarray) -> bytes:
    """
    Serializa um embedding para armazenamento no banco de dados.

    A serialização também valida dimensão, valores e norma para impedir
    a gravação de novos registros inconsistentes.
    """

    array = np.asarray(
        vector,
        dtype=np.float32,
    ).reshape(-1)

    if array.size != EMBEDDING_DIM:
        raise ValueError(
            "Tentativa de serializar embedding com dimensão "
            f"{array.size}; esperado={EMBEDDING_DIM}"
        )

    if not np.all(np.isfinite(array)):
        raise ValueError(
            "Tentativa de serializar embedding contendo NaN ou infinito"
        )

    if float(np.linalg.norm(array)) == 0:
        raise ValueError(
            "Tentativa de serializar embedding com norma zero"
        )

    return np.ascontiguousarray(
        array,
        dtype=np.float32,
    ).tobytes()


def decode_embedding(blob: bytes) -> np.ndarray:
    """
    Desserializa e valida um embedding SFace.

    Diferentemente da implementação anterior, esta função não confia
    na dimensão armazenada na linha do banco. O pipeline atual aceita
    exclusivamente embeddings SFace de 128 dimensões em float32.
    """

    if blob is None or len(blob) == 0:
        raise ValueError("Embedding vazio")

    expected_bytes = EMBEDDING_DIM * np.dtype(
        np.float32
    ).itemsize

    if len(blob) != expected_bytes:
        raise ValueError(
            "Tamanho binário incompatível: "
            f"recebido={len(blob)} bytes, "
            f"esperado={expected_bytes} bytes"
        )

    array = np.frombuffer(
        blob,
        dtype=np.float32,
    )

    if array.size != EMBEDDING_DIM:
        raise ValueError(
            "Embedding size mismatch: "
            f"recebido={array.size}, "
            f"esperado={EMBEDDING_DIM}"
        )

    if not np.all(np.isfinite(array)):
        raise ValueError(
            "Embedding contém NaN ou infinito"
        )

    if float(np.linalg.norm(array)) == 0:
        raise ValueError(
            "Embedding possui norma zero"
        )

    return np.ascontiguousarray(
        array,
        dtype=np.float32,
    ).reshape(EMBEDDING_DIM)


# ---------------------------------------------------------------------------
# Índice facial em memória
# ---------------------------------------------------------------------------

def invalidate_index() -> None:
    """Marca o índice facial para recarregamento."""

    global _emb_dirty

    with _index_lock:
        _emb_dirty = True

    LOGGER.debug("Índice facial marcado como desatualizado")


def _load_index() -> None:
    """
    Carrega os embeddings faciais válidos do banco.

    Esta função deve ser chamada com `_index_lock` adquirido.

    Embeddings incompatíveis são ignorados e registrados no log.
    Somente vetores de 128 dimensões, float32, finitos e com norma
    diferente de zero são incluídos no índice.
    """

    global _emb_matrix
    global _emb_employee_ids
    global _emb_norms
    global _emb_dirty

    rows = db.list_face_embeddings()

    if not rows:
        _emb_matrix = None
        _emb_employee_ids = []
        _emb_norms = None
        _emb_dirty = False

        LOGGER.info(
            "Nenhum embedding facial cadastrado"
        )
        return

    vectors: list[np.ndarray] = []
    employee_ids: list[str] = []

    invalid_count = 0

    dimension_counts: dict[int, int] = {}

    for row in rows:
        embedding_id = row.get("id")
        employee_id = row.get("employee_id")
        stored_dim = row.get("dim")
        stored_dtype = row.get("dtype")

        try:
            if employee_id is None:
                raise ValueError(
                    "employee_id não informado"
                )

            if stored_dim is None:
                raise ValueError(
                    "Dimensão não informada no banco"
                )

            stored_dim_int = int(stored_dim)

            dimension_counts[stored_dim_int] = (
                dimension_counts.get(stored_dim_int, 0) + 1
            )

            if stored_dim_int != EMBEDDING_DIM:
                raise ValueError(
                    "Dimensão incompatível: "
                    f"banco={stored_dim_int}, "
                    f"esperada={EMBEDDING_DIM}"
                )

            if stored_dtype is not None:
                normalized_dtype = str(
                    stored_dtype
                ).strip().lower()

                if normalized_dtype not in {
                    EMBEDDING_DTYPE,
                    "<f4",
                    "float",
                }:
                    raise ValueError(
                        "Dtype incompatível: "
                        f"banco={stored_dtype}, "
                        f"esperado={EMBEDDING_DTYPE}"
                    )

            blob = row.get("vec")

            vector = decode_embedding(blob)

            if vector.shape != (EMBEDDING_DIM,):
                raise ValueError(
                    "Shape incompatível: "
                    f"recebido={vector.shape}, "
                    f"esperado=({EMBEDDING_DIM},)"
                )

            vectors.append(vector)
            employee_ids.append(str(employee_id))

        except Exception as exc:
            invalid_count += 1

            LOGGER.warning(
                "Embedding facial inválido ignorado: "
                "id=%s employee_id=%s dim=%s dtype=%s motivo=%s",
                embedding_id,
                employee_id,
                stored_dim,
                stored_dtype,
                exc,
            )

    LOGGER.info(
        "Dimensões encontradas nos embeddings do banco: %s",
        dimension_counts,
    )

    if not vectors:
        _emb_matrix = None
        _emb_employee_ids = []
        _emb_norms = None
        _emb_dirty = False

        LOGGER.error(
            "Nenhum embedding facial válido encontrado. "
            "Total de registros=%d inválidos=%d",
            len(rows),
            invalid_count,
        )
        return

    # Neste ponto todos os vetores já foram validados com shape (128,).
    matrix = np.stack(
        vectors,
        axis=0,
    ).astype(
        np.float32,
        copy=False,
    )

    expected_matrix_shape = (
        len(vectors),
        EMBEDDING_DIM,
    )

    if matrix.shape != expected_matrix_shape:
        raise RuntimeError (
            "Formato inesperado da matriz facial: "
            f"recebido={matrix.shape}, "
            f"esperado={expected_matrix_shape}"
        )

    norms = np.linalg.norm(
        matrix,
        axis=1,
    ).astype(
        np.float32,
        copy=False,
    )

    valid_norm_mask = (
        np.isfinite(norms)
        & (norms > 0)
    )

    if not np.all(valid_norm_mask):
        invalid_indexes = np.flatnonzero(
            ~valid_norm_mask
        ).tolist()

        LOGGER.error(
            "Embeddings com norma inválida após montagem "
            "do índice: indexes=%s",
            invalid_indexes,
        )

        # Remove simultaneamente os vetores e IDs inválidos,
        # preservando a correspondência entre ambos.
        matrix = matrix[valid_norm_mask]
        norms = norms[valid_norm_mask]

        employee_ids = [
            employee_id
            for index, employee_id in enumerate(employee_ids)
            if bool(valid_norm_mask[index])
        ]

    if matrix.shape[0] == 0:
        _emb_matrix = None
        _emb_employee_ids = []
        _emb_norms = None
        _emb_dirty = False

        LOGGER.error(
            "Nenhum embedding permaneceu válido após "
            "a validação das normas"
        )
        return

    if matrix.shape[0] != len(employee_ids):
        raise RuntimeError(
            "Índice facial inconsistente: "
            f"vetores={matrix.shape[0]}, "
            f"employee_ids={len(employee_ids)}"
        )

    if matrix.shape[0] != norms.shape[0]:
        raise RuntimeError(
            "Índice facial inconsistente: "
            f"vetores={matrix.shape[0]}, "
            f"normas={norms.shape[0]}"
        )

    _emb_matrix = matrix
    _emb_employee_ids = employee_ids
    _emb_norms = norms
    _emb_dirty = False

    LOGGER.info(
        "Índice facial carregado: "
        "válidos=%d inválidos=%d shape=%s",
        matrix.shape[0],
        invalid_count,
        matrix.shape,
    )


def _ensure_index() -> None:
    """Carrega ou atualiza o índice facial quando necessário."""

    with _index_lock:
        if _emb_dirty:
            _load_index()


def warm_up_models() -> None:
    """
    Carrega YuNet, SFace e o índice de embeddings antes do primeiro uso.
    """

    _get_detector(
        (
            config.FRAME_WIDTH,
            config.FRAME_HEIGHT,
        )
    )

    _get_recognizer()
    _ensure_index()

    LOGGER.info(
        "Modelos faciais e índice inicializados"
    )


# ---------------------------------------------------------------------------
# Matching
# ---------------------------------------------------------------------------

@dataclass
class MatchResult:
    """Resultado do matching facial."""

    employee_id: str | None
    score: float
    threshold: float
    known: bool


def _unknown_match(
    threshold: float,
    score: float = -1.0,
) -> MatchResult:
    """Cria um resultado facial desconhecido."""

    return MatchResult(
        employee_id=None,
        score=score,
        threshold=threshold,
        known=False,
    )


def match_embedding(vector: np.ndarray) -> MatchResult:
    """
    Compara um embedding com o índice facial.

    Para colaboradores com múltiplos embeddings, o maior score de cada
    colaborador é considerado. Em seguida, o colaborador com maior score
    geral é selecionado.
    """

    _ensure_index()

    threshold = float(
        config.SFACE_COSINE_THRESHOLD
    )

    if (
        _emb_matrix is None
        or _emb_norms is None
        or not _emb_employee_ids
    ):
        LOGGER.info(
            "Matching não executado: índice facial vazio"
        )
        return _unknown_match(threshold)

    candidate = np.asarray(
        vector,
        dtype=np.float32,
    ).reshape(-1)

    if candidate.size != EMBEDDING_DIM:
        LOGGER.error(
            "Embedding recebido para matching possui dimensão %d; "
            "esperado=%d",
            candidate.size,
            EMBEDDING_DIM,
        )
        return _unknown_match(threshold)

    if not np.all(np.isfinite(candidate)):
        LOGGER.error(
            "Embedding recebido para matching contém NaN ou infinito"
        )
        return _unknown_match(threshold)

    candidate_norm = float(
        np.linalg.norm(candidate)
    )

    if not np.isfinite(candidate_norm) or candidate_norm == 0:
        LOGGER.error(
            "Embedding recebido para matching possui norma inválida: %s",
            candidate_norm,
        )
        return _unknown_match(threshold)

    if _emb_matrix.shape[1] != EMBEDDING_DIM:
        LOGGER.error(
            "Matriz facial em memória possui shape inválido: %s",
            _emb_matrix.shape,
        )
        return _unknown_match(threshold)

    if _emb_matrix.shape[0] != len(_emb_employee_ids):
        LOGGER.error(
            "Índice facial desalinhado: vetores=%d IDs=%d",
            _emb_matrix.shape[0],
            len(_emb_employee_ids),
        )
        return _unknown_match(threshold)

    similarities = (
        (_emb_matrix @ candidate)
        / (_emb_norms * candidate_norm)
    )

    best_by_employee: dict[str, float] = {}

    for employee_id, similarity in zip(
        _emb_employee_ids,
        similarities,
        strict=False,
    ):
        score = float(similarity)

        if not np.isfinite(score):
            LOGGER.warning(
                "Similaridade inválida ignorada: employee_id=%s score=%s",
                employee_id,
                score,
            )
            continue

        previous_score = best_by_employee.get(employee_id)

        if previous_score is None or score > previous_score:
            best_by_employee[employee_id] = score

    if not best_by_employee:
        LOGGER.warning(
            "Nenhuma similaridade facial válida foi calculada"
        )
        return _unknown_match(threshold)

    employee_id, score = max(
        best_by_employee.items(),
        key=lambda item: item[1],
    )

    return MatchResult(
        employee_id=employee_id,
        score=score,
        threshold=threshold,
        known=score >= threshold,
    )


# ---------------------------------------------------------------------------
# Cadastro facial
# ---------------------------------------------------------------------------

def register_face_from_frame(
    employee_id: str,
    frame_bgr: np.ndarray,
) -> DetectedFace | None:
    """
    Detecta a maior face do frame, gera o embedding e persiste no banco.

    Retorna a face detectada ou `None` quando nenhuma face for encontrada.
    """

    if not employee_id or not str(employee_id).strip():
        raise ValueError(
            "employee_id é obrigatório para o cadastro facial"
        )

    face = detect_largest_face(frame_bgr)

    if face is None:
        return None

    vector = compute_embedding(face)

    db.add_face_embedding(
        employee_id=str(employee_id),
        vec_bytes=encode_embedding(vector),
        dim=EMBEDDING_DIM,
        dtype=EMBEDDING_DTYPE,
    )

    invalidate_index()

    LOGGER.info(
        "Embedding facial registrado: "
        "employee_id=%s dim=%d dtype=%s",
        employee_id,
        EMBEDDING_DIM,
        EMBEDDING_DTYPE,
    )

    return face


# ---------------------------------------------------------------------------
# Reconstrução do índice por dataset
# ---------------------------------------------------------------------------

def rebuild_index_from_dataset() -> dict[str, int]:
    """
    Recomputa todos os embeddings usando as imagens do dataset.

    Estrutura esperada:

        data/dataset/<employee_id>/*.png
        data/dataset/<employee_id>/*.jpg
        data/dataset/<employee_id>/*.jpeg

    Atenção:
        Esta função limpa os embeddings faciais atuais antes de reconstruí-los.
    """

    config.ensure_directories()
    ensure_models()

    if not config.DATASET_DIR.exists():
        raise FileNotFoundError(
            f"Diretório de dataset não encontrado: {config.DATASET_DIR}"
        )

    LOGGER.warning(
        "Iniciando reconstrução completa do índice facial"
    )

    db.clear_face_embeddings()

    stats: dict[str, int] = {}

    for person_dir in sorted(
        config.DATASET_DIR.iterdir()
    ):
        if not person_dir.is_dir():
            continue

        employee_id = person_dir.name
        count = 0

        for image_path in sorted(
            person_dir.iterdir()
        ):
            if image_path.suffix.lower() not in {
                ".png",
                ".jpg",
                ".jpeg",
            }:
                continue

            image = cv2.imread(
                str(image_path),
                cv2.IMREAD_COLOR,
            )

            if image is None:
                LOGGER.warning(
                    "Imagem inválida ou não carregada: %s",
                    image_path,
                )
                continue

            try:
                face = detect_largest_face(image)

                if face is None:
                    LOGGER.warning(
                        "Nenhuma face detectada em %s",
                        image_path,
                    )
                    continue

                vector = compute_embedding(face)

                db.add_face_embedding(
                    employee_id=employee_id,
                    vec_bytes=encode_embedding(vector),
                    dim=EMBEDDING_DIM,
                    dtype=EMBEDDING_DTYPE,
                )

                count += 1

            except Exception:
                LOGGER.exception(
                    "Falha ao processar imagem do dataset: %s",
                    image_path,
                )

        stats[employee_id] = count

        LOGGER.info(
            "Reconstrução facial: employee_id=%s embeddings=%d",
            employee_id,
            count,
        )

    invalidate_index()

    LOGGER.info(
        "Reconstrução do índice facial concluída: %s",
        stats,
    )

    return stats


# ---------------------------------------------------------------------------
# Overlay visual
# ---------------------------------------------------------------------------

def draw_overlay(
    frame: np.ndarray,
    face: DetectedFace,
    label: str,
    score: float | None,
    known: bool,
) -> None:
    """Desenha bounding box, landmarks, nome e score sobre o frame."""

    x, y, width, height = face.bbox

    color = (
        (0, 180, 0)
        if known
        else (0, 0, 255)
    )

    cv2.rectangle(
        frame,
        (x, y),
        (x + width, y + height),
        color,
        2,
    )

    for point_x, point_y in face.keypoints:
        cv2.circle(
            frame,
            (point_x, point_y),
            2,
            (255, 200, 0),
            -1,
        )

    score_text = ""

    if score is not None and np.isfinite(score):
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