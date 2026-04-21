"""Reconstroi o indice de embeddings SFace a partir de `data/dataset/`."""
from __future__ import annotations

import logging

from . import config
from . import db
from .services.face_engine import rebuild_index_from_dataset

LOGGER = logging.getLogger(__name__)


def train_model() -> dict[str, int]:
    """Recomputa embeddings SFace. Retorna dict employee_id -> qtd."""
    config.ensure_directories()
    stats = rebuild_index_from_dataset()
    LOGGER.info("Indice SFace reconstruido: %s", stats)
    return stats


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    db.init_db()
    train_model()
