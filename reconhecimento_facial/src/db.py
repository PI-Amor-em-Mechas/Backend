"""Camada de acesso ao banco MariaDB / MySQL com fallback para SQLite.

O schema e criado externamente via `data/schema.sql` (MySQL). Quando
DB_BACKEND=sqlite, o schema e criado automaticamente em SQLite local.

Configuracao por variaveis de ambiente (ver `config.py`):
    DB_BACKEND (mysql|sqlite), DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME, DB_CHARSET
"""
from __future__ import annotations

import logging
import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Any, Iterator

from . import config

LOGGER = logging.getLogger(__name__)

ANONYMIZED_MARKER = "__ANON__"

_USE_SQLITE = config.DB_BACKEND == "sqlite"


# ---------------------------------------------------------------------------
# SQLite helpers
# ---------------------------------------------------------------------------

_SQLITE_PATH = config.DATA_DIR / "local.db"


def _sqlite_dict_factory(cursor: sqlite3.Cursor, row: tuple) -> dict:
    cols = [col[0] for col in cursor.description]
    return dict(zip(cols, row))


def _sqlite_connect() -> sqlite3.Connection:
    conn = sqlite3.connect(str(_SQLITE_PATH), timeout=10)
    conn.row_factory = _sqlite_dict_factory
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


_SQLITE_SCHEMA = """\
CREATE TABLE IF NOT EXISTS colaborador (
    id              TEXT PRIMARY KEY,
    nome            TEXT NOT NULL,
    criado_em       TEXT NOT NULL,
    anonimizado_em  TEXT
);
CREATE TABLE IF NOT EXISTS embedding_facial (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id TEXT NOT NULL REFERENCES colaborador(id),
    vetor          BLOB NOT NULL,
    dimensao       INTEGER NOT NULL,
    dtype          TEXT NOT NULL,
    criado_em      TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS ponto (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id  TEXT NOT NULL REFERENCES colaborador(id),
    data_hora       TEXT NOT NULL,
    tipo            TEXT NOT NULL,
    confianca       REAL NOT NULL,
    caminho_imagem  TEXT
);
CREATE TABLE IF NOT EXISTS consentimento (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id  TEXT NOT NULL REFERENCES colaborador(id),
    versao          TEXT NOT NULL,
    consentido_em   TEXT NOT NULL,
    revogado_em     TEXT,
    user_agent      TEXT,
    ip              TEXT
);
CREATE TABLE IF NOT EXISTS comando_voz (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id  TEXT NOT NULL REFERENCES colaborador(id),
    texto           TEXT NOT NULL,
    criado_em       TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS embedding_voz (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id TEXT NOT NULL REFERENCES colaborador(id),
    vetor          BLOB NOT NULL,
    dimensao       INTEGER NOT NULL,
    dtype          TEXT NOT NULL,
    criado_em      TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS log_auditoria (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    data_hora TEXT NOT NULL,
    autor     TEXT NOT NULL,
    acao      TEXT NOT NULL,
    alvo      TEXT,
    ip        TEXT,
    detalhes  TEXT
);
"""


# ---------------------------------------------------------------------------
# MySQL helpers
# ---------------------------------------------------------------------------

def _mysql_connect():
    import pymysql
    from pymysql.cursors import DictCursor
    return pymysql.connect(
        host=config.DB_HOST,
        port=config.DB_PORT,
        user=config.DB_USER,
        password=config.DB_PASSWORD,
        database=config.DB_NAME,
        charset=config.DB_CHARSET,
        cursorclass=DictCursor,
        autocommit=False,
    )


# ---------------------------------------------------------------------------
# Unified connection context
# ---------------------------------------------------------------------------

@contextmanager
def _conn_ctx() -> Iterator[Any]:
    if _USE_SQLITE:
        conn = _sqlite_connect()
        try:
            yield conn
        finally:
            conn.close()
    else:
        conn = _mysql_connect()
        try:
            yield conn
        finally:
            conn.close()


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")


def _ph(count: int = 1) -> str:
    """Retorna placeholder(s) adequado(s) ao backend."""
    p = "?" if _USE_SQLITE else "%s"
    return ", ".join([p] * count) if count > 1 else p


def _execute(conn, sql: str, params: tuple = ()) -> Any:
    """Executa SQL adaptando placeholder conforme backend."""
    if _USE_SQLITE:
        return conn.execute(sql, params)
    else:
        cur = conn.cursor()
        cur.execute(sql, params)
        return cur


def init_db() -> None:
    """Inicializa o banco. SQLite cria schema; MySQL verifica tabelas."""
    if _USE_SQLITE:
        config.DATA_DIR.mkdir(parents=True, exist_ok=True)
        with _conn_ctx() as conn:
            conn.executescript(_SQLITE_SCHEMA)
        LOGGER.info("SQLite inicializado em %s — schema OK.", _SQLITE_PATH)
        return

    import pymysql
    required_tables = {
        "colaborador",
        "ponto",
        "comando_voz",
        "consentimento",
        "log_auditoria",
        "embedding_facial",
        "embedding_voz",
    }
    try:
        with _conn_ctx() as conn:
            with conn.cursor() as cur:
                cur.execute("SHOW TABLES")
                existing = {next(iter(row.values())) for row in cur.fetchall()}
    except pymysql.err.OperationalError as exc:
        raise RuntimeError(
            f"Nao foi possivel conectar ao banco {config.DB_HOST}:{config.DB_PORT} "
            f"(db={config.DB_NAME}, user={config.DB_USER}). Erro: {exc}"
        ) from exc

    missing = required_tables - existing
    if missing:
        raise RuntimeError(
            "Tabelas ausentes no banco: "
            + ", ".join(sorted(missing))
            + ". Importe data/schema.sql antes de subir a aplicacao."
        )
    LOGGER.info(
        "Conectado ao MySQL %s:%s/%s — schema OK.",
        config.DB_HOST, config.DB_PORT, config.DB_NAME,
    )


# ---------------------------------------------------------------------------
# colaborador (employees)
# ---------------------------------------------------------------------------

def add_employee(employee_id: str, name: str) -> None:
    now = _utc_now_iso()
    if _USE_SQLITE:
        sql = "INSERT OR REPLACE INTO colaborador (id, nome, criado_em) VALUES (?, ?, ?)"
    else:
        sql = (
            "INSERT INTO colaborador (id, nome, criado_em) VALUES (%s, %s, %s) "
            "ON DUPLICATE KEY UPDATE nome = VALUES(nome)"
        )
    with _conn_ctx() as conn:
        _execute(conn, sql, (employee_id, name, now))
        conn.commit()


def get_employee(employee_id: str) -> dict[str, Any] | None:
    ph = _ph()
    sql = (
        f"SELECT id, nome AS name, criado_em AS created_at, "
        f"anonimizado_em AS anonymized_at FROM colaborador WHERE id = {ph}"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        row = cur.fetchone()
    return dict(row) if row else None


def list_employees(include_anonymized: bool = False) -> list[dict[str, Any]]:
    if include_anonymized:
        sql = (
            "SELECT id, nome AS name, criado_em AS created_at, "
            "anonimizado_em AS anonymized_at FROM colaborador ORDER BY nome"
        )
        params: tuple = ()
    else:
        sql = (
            "SELECT id, nome AS name, criado_em AS created_at, "
            "anonimizado_em AS anonymized_at FROM colaborador "
            "WHERE anonimizado_em IS NULL ORDER BY nome"
        )
        params = ()
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, params)
        rows = cur.fetchall()
    return [dict(r) for r in rows]


def delete_employee(employee_id: str) -> int:
    """Remove fisicamente o registro. Prefira `anonymize_employee` para manter
    integridade historica de pontos."""
    ph = _ph()
    with _conn_ctx() as conn:
        cur = _execute(conn, f"DELETE FROM colaborador WHERE id = {ph}", (employee_id,))
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


def anonymize_employee(employee_id: str) -> bool:
    """Anonimiza colaborador: substitui nome por marcador, marca timestamp,
    remove comandos de voz e caminho de imagens. Mantem o historico de pontos
    para fins contabeis/legais."""
    now = _utc_now_iso()
    ph = _ph()
    with _conn_ctx() as conn:
        cur = _execute(
            conn,
            f"UPDATE colaborador SET nome = {ph}, anonimizado_em = {ph} "
            f"WHERE id = {ph} AND anonimizado_em IS NULL",
            (ANONYMIZED_MARKER, now, employee_id),
        )
        updated = cur.rowcount
        _execute(conn, f"DELETE FROM comando_voz WHERE colaborador_id = {ph}", (employee_id,))
        _execute(conn, f"UPDATE ponto SET caminho_imagem = NULL WHERE colaborador_id = {ph}", (employee_id,))
        _execute(conn, f"DELETE FROM embedding_facial WHERE colaborador_id = {ph}", (employee_id,))
        _execute(conn, f"DELETE FROM embedding_voz WHERE colaborador_id = {ph}", (employee_id,))
        conn.commit()
    return updated > 0


# ---------------------------------------------------------------------------
# ponto (punches)
# ---------------------------------------------------------------------------

def add_punch(
    employee_id: str,
    punch_type: str,
    confidence: float,
    image_path: str | None = None,
    ts: str | None = None,
) -> int:
    ts_value = ts or _utc_now_iso()
    ph = _ph(5)
    sql = (
        f"INSERT INTO ponto (colaborador_id, data_hora, tipo, confianca, caminho_imagem) "
        f"VALUES ({ph})"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id, ts_value, punch_type, float(confidence), image_path))
        new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def get_last_punch(employee_id: str) -> dict[str, Any] | None:
    ph = _ph()
    sql = (
        f"SELECT id, colaborador_id AS employee_id, data_hora AS ts, "
        f"tipo AS type, confianca AS confidence, caminho_imagem AS image_path "
        f"FROM ponto WHERE colaborador_id = {ph} ORDER BY data_hora DESC LIMIT 1"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        row = cur.fetchone()
    return dict(row) if row else None


def list_punches(limit: int = 100) -> list[dict[str, Any]]:
    ph = _ph()
    sql = (
        f"SELECT p.id, p.colaborador_id AS employee_id, c.nome AS name, "
        f"p.data_hora AS ts, p.tipo AS type, p.confianca AS confidence, "
        f"p.caminho_imagem AS image_path "
        f"FROM ponto p LEFT JOIN colaborador c ON c.id = p.colaborador_id "
        f"ORDER BY p.data_hora DESC LIMIT {ph}"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (int(limit),))
        rows = cur.fetchall()
    return [dict(r) for r in rows]


def list_punches_for(employee_id: str) -> list[dict[str, Any]]:
    ph = _ph()
    sql = (
        f"SELECT id, colaborador_id AS employee_id, data_hora AS ts, "
        f"tipo AS type, confianca AS confidence, caminho_imagem AS image_path "
        f"FROM ponto WHERE colaborador_id = {ph} ORDER BY data_hora ASC"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        rows = cur.fetchall()
    return [dict(r) for r in rows]


def count_punches_between(employee_id: str, start_utc_iso: str, end_utc_iso: str) -> int:
    """Conta pontos do colaborador em um intervalo UTC [start, end)."""
    ph = _ph()
    sql = (
        f"SELECT COUNT(*) AS c FROM ponto "
        f"WHERE colaborador_id = {ph} AND data_hora >= {ph} AND data_hora < {ph}"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id, start_utc_iso, end_utc_iso))
        row = cur.fetchone()
    return int(row["c"]) if row else 0


def collect_punch_image_paths_before(cutoff_utc_iso: str) -> list[str]:
    """Retorna caminhos de imagem anteriores ao cutoff e zera a coluna no banco."""
    ph = _ph()
    select_sql = (
        f"SELECT caminho_imagem AS image_path FROM ponto "
        f"WHERE caminho_imagem IS NOT NULL AND data_hora < {ph}"
    )
    update_sql = (
        f"UPDATE ponto SET caminho_imagem = NULL "
        f"WHERE caminho_imagem IS NOT NULL AND data_hora < {ph}"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, select_sql, (cutoff_utc_iso,))
        rows = cur.fetchall()
        paths = [r["image_path"] for r in rows if r["image_path"]]
        if paths:
            _execute(conn, update_sql, (cutoff_utc_iso,))
        conn.commit()
    return paths


def delete_punches_before(cutoff_utc_iso: str) -> int:
    ph = _ph()
    with _conn_ctx() as conn:
        cur = _execute(conn, f"DELETE FROM ponto WHERE data_hora < {ph}", (cutoff_utc_iso,))
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


# ---------------------------------------------------------------------------
# comando_voz (voice_commands)
# ---------------------------------------------------------------------------

def add_voice_command(employee_id: str, text: str) -> int:
    now = _utc_now_iso()
    ph = _ph(3)
    sql = f"INSERT INTO comando_voz (colaborador_id, texto, criado_em) VALUES ({ph})"
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id, text, now))
        new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def list_voice_commands(
    employee_id: str | None = None, limit: int = 50
) -> list[dict[str, Any]]:
    ph = _ph()
    base = (
        "SELECT vc.id, vc.colaborador_id AS employee_id, c.nome AS name, "
        "vc.texto AS text, vc.criado_em AS created_at "
        "FROM comando_voz vc "
        "LEFT JOIN colaborador c ON c.id = vc.colaborador_id "
    )
    with _conn_ctx() as conn:
        if employee_id:
            cur = _execute(
                conn,
                base + f"WHERE vc.colaborador_id = {ph} ORDER BY vc.criado_em DESC LIMIT {ph}",
                (employee_id, int(limit)),
            )
        else:
            cur = _execute(
                conn,
                base + f"ORDER BY vc.criado_em DESC LIMIT {ph}",
                (int(limit),),
            )
        rows = cur.fetchall()
    return [dict(r) for r in rows]


# ---------------------------------------------------------------------------
# consentimento (consents)
# ---------------------------------------------------------------------------

def add_consent(
    employee_id: str,
    version: str,
    user_agent: str | None = None,
    ip: str | None = None,
) -> int:
    now = _utc_now_iso()
    ph = _ph(5)
    sql = (
        f"INSERT INTO consentimento (colaborador_id, versao, consentido_em, user_agent, ip) "
        f"VALUES ({ph})"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id, version, now, user_agent, ip))
        new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def revoke_consents(employee_id: str) -> int:
    now = _utc_now_iso()
    ph = _ph()
    sql = (
        f"UPDATE consentimento SET revogado_em = {ph} "
        f"WHERE colaborador_id = {ph} AND revogado_em IS NULL"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (now, employee_id))
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


def latest_consent(employee_id: str) -> dict[str, Any] | None:
    ph = _ph()
    sql = (
        f"SELECT id, colaborador_id AS employee_id, versao AS version, "
        f"consentido_em AS consented_at, revogado_em AS revoked_at, user_agent, ip "
        f"FROM consentimento WHERE colaborador_id = {ph} "
        f"ORDER BY consentido_em DESC LIMIT 1"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        row = cur.fetchone()
    return dict(row) if row else None


def list_consents(employee_id: str) -> list[dict[str, Any]]:
    ph = _ph()
    sql = (
        f"SELECT id, colaborador_id AS employee_id, versao AS version, "
        f"consentido_em AS consented_at, revogado_em AS revoked_at, user_agent, ip "
        f"FROM consentimento WHERE colaborador_id = {ph} ORDER BY consentido_em DESC"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        rows = cur.fetchall()
    return [dict(r) for r in rows]


# ---------------------------------------------------------------------------
# log_auditoria (audit_log)
# ---------------------------------------------------------------------------

def add_audit(
    actor: str,
    action: str,
    target: str | None = None,
    ip: str | None = None,
    details: str | None = None,
) -> int:
    now = _utc_now_iso()
    ph = _ph(6)
    sql = (
        f"INSERT INTO log_auditoria (data_hora, autor, acao, alvo, ip, detalhes) "
        f"VALUES ({ph})"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (now, actor, action, target, ip, details))
        new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def list_audit(limit: int = 200) -> list[dict[str, Any]]:
    ph = _ph()
    sql = (
        f"SELECT id, data_hora AS ts, autor AS actor, acao AS action, "
        f"alvo AS target, ip, detalhes AS details "
        f"FROM log_auditoria ORDER BY data_hora DESC LIMIT {ph}"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (int(limit),))
        rows = cur.fetchall()
    return [dict(r) for r in rows]


def delete_audit_before(cutoff_utc_iso: str) -> int:
    ph = _ph()
    with _conn_ctx() as conn:
        cur = _execute(conn, f"DELETE FROM log_auditoria WHERE data_hora < {ph}", (cutoff_utc_iso,))
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


# ---------------------------------------------------------------------------
# embedding_facial (face_embeddings)
# ---------------------------------------------------------------------------

def add_face_embedding(employee_id: str, vec_bytes: bytes, dim: int, dtype: str) -> int:
    now = _utc_now_iso()
    ph = _ph(5)
    sql = (
        f"INSERT INTO embedding_facial (colaborador_id, vetor, dimensao, dtype, criado_em) "
        f"VALUES ({ph})"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id, vec_bytes, int(dim), dtype, now))
        new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def list_face_embeddings() -> list[dict[str, Any]]:
    """Retorna embeddings apenas de colaboradores nao-anonimizados."""
    sql = (
        "SELECT ef.id, ef.colaborador_id AS employee_id, ef.vetor AS vec, "
        "ef.dimensao AS dim, ef.dtype, ef.criado_em AS created_at "
        "FROM embedding_facial ef "
        "INNER JOIN colaborador c ON c.id = ef.colaborador_id "
        "WHERE c.anonimizado_em IS NULL"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql)
        rows = cur.fetchall()
    return [dict(r) for r in rows]


def count_face_embeddings(employee_id: str) -> int:
    ph = _ph()
    sql = f"SELECT COUNT(*) AS c FROM embedding_facial WHERE colaborador_id = {ph}"
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        row = cur.fetchone()
    return int(row["c"]) if row else 0


def delete_face_embeddings(employee_id: str) -> int:
    ph = _ph()
    with _conn_ctx() as conn:
        cur = _execute(conn, f"DELETE FROM embedding_facial WHERE colaborador_id = {ph}", (employee_id,))
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


def clear_face_embeddings() -> int:
    with _conn_ctx() as conn:
        cur = _execute(conn, "DELETE FROM embedding_facial")
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


# ---------------------------------------------------------------------------
# embedding_voz (voice biometrics — Resemblyzer / GE2E)
# ---------------------------------------------------------------------------

def add_voice_embedding(
    employee_id: str, vec_bytes: bytes, dim: int, dtype: str
) -> int:
    now = _utc_now_iso()
    ph = _ph(5)
    sql = (
        f"INSERT INTO embedding_voz (colaborador_id, vetor, dimensao, dtype, criado_em) "
        f"VALUES ({ph})"
    )
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id, vec_bytes, int(dim), dtype, now))
        new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def list_voice_embeddings(employee_id: str | None = None) -> list[dict[str, Any]]:
    """Retorna voiceprints, ignorando colaboradores anonimizados."""
    ph = _ph()
    base = (
        "SELECT ev.id, ev.colaborador_id AS employee_id, ev.vetor AS vec, "
        "ev.dimensao AS dim, ev.dtype, ev.criado_em AS created_at "
        "FROM embedding_voz ev "
        "INNER JOIN colaborador c ON c.id = ev.colaborador_id "
        "WHERE c.anonimizado_em IS NULL"
    )
    with _conn_ctx() as conn:
        if employee_id:
            cur = _execute(conn, base + f" AND ev.colaborador_id = {ph}", (employee_id,))
        else:
            cur = _execute(conn, base)
        rows = cur.fetchall()
    return [dict(r) for r in rows]


def count_voice_embeddings(employee_id: str) -> int:
    ph = _ph()
    sql = f"SELECT COUNT(*) AS c FROM embedding_voz WHERE colaborador_id = {ph}"
    with _conn_ctx() as conn:
        cur = _execute(conn, sql, (employee_id,))
        row = cur.fetchone()
    return int(row["c"]) if row else 0


def delete_voice_embeddings(employee_id: str) -> int:
    ph = _ph()
    with _conn_ctx() as conn:
        cur = _execute(conn, f"DELETE FROM embedding_voz WHERE colaborador_id = {ph}", (employee_id,))
        rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)
