"""Locks compartilhados entre rotas e handlers.

Com a migracao para SFace (singleton em face_engine), este modulo mantem
apenas o lock global usado para operacoes de rebuild de embeddings.
"""
from __future__ import annotations

import threading

_train_lock = threading.Lock()

def train_lock() -> threading.Lock:
    return _train_lock
