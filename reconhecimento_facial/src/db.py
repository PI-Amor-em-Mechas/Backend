"""Camada de acesso ao banco MariaDB / MySQL.

O schema e criado externamente via `data/schema.sql`. Esta camada apenas
abre conexoes e executa queries usando PyMySQL.

Configuracao por variaveis de ambiente (ver `config.py`):
    DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME, DB_CHARSET
"""
from __future__ import annotations

import logging
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Any, Iterator

import pymysql
from pymysql.cursors import DictCursor

from . import config

LOGGER = logging.getLogger(__name__)

ANONYMIZED_MARKER = "__ANON__"


def _connect() -> pymysql.connections.Connection:
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


@contextmanager
def _conn_ctx() -> Iterator[pymysql.connections.Connection]:
    conn = _connect()
    try:
        yield conn
    finally:
        conn.close()


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")


def init_db() -> None:
    """Sanity check: verifica que o schema esperado existe.

    O schema deve ser criado previamente via `data/schema.sql`.
    """
    required_tables = {
        "colaborador",
        "ponto",
        "comando_voz",
        "consentimento",
        "log_auditoria",
        "embedding_facial",
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
    sql = (
        "INSERT INTO colaborador (id, nome, criado_em) VALUES (%s, %s, %s) "
        "ON DUPLICATE KEY UPDATE nome = VALUES(nome)"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id, name, now))
        conn.commit()


def get_employee(employee_id: str) -> dict[str, Any] | None:
    sql = (
        "SELECT id, nome AS name, criado_em AS created_at, "
        "anonimizado_em AS anonymized_at FROM colaborador WHERE id = %s"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id,))
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
        with conn.cursor() as cur:
            cur.execute(sql, params)
            rows = cur.fetchall()
    return [dict(r) for r in rows]


def delete_employee(employee_id: str) -> int:
    """Remove fisicamente o registro. Prefira `anonymize_employee` para manter
    integridade historica de pontos."""
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM colaborador WHERE id = %s", (employee_id,))
            rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


def anonymize_employee(employee_id: str) -> bool:
    """Anonimiza colaborador: substitui nome por marcador, marca timestamp,
    remove comandos de voz e caminho de imagens. Mantem o historico de pontos
    para fins contabeis/legais."""
    now = _utc_now_iso()
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                UPDATE colaborador
                SET nome = %s, anonimizado_em = %s
                WHERE id = %s AND anonimizado_em IS NULL
                """,
                (ANONYMIZED_MARKER, now, employee_id),
            )
            updated = cur.rowcount
            cur.execute(
                "DELETE FROM comando_voz WHERE colaborador_id = %s",
                (employee_id,),
            )
            cur.execute(
                "UPDATE ponto SET caminho_imagem = NULL WHERE colaborador_id = %s",
                (employee_id,),
            )
            cur.execute(
                "DELETE FROM embedding_facial WHERE colaborador_id = %s",
                (employee_id,),
            )
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
    sql = (
        "INSERT INTO ponto (colaborador_id, data_hora, tipo, confianca, caminho_imagem) "
        "VALUES (%s, %s, %s, %s, %s)"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id, ts_value, punch_type, float(confidence), image_path))
            new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def get_last_punch(employee_id: str) -> dict[str, Any] | None:
    sql = (
        "SELECT id, colaborador_id AS employee_id, data_hora AS ts, "
        "tipo AS type, confianca AS confidence, caminho_imagem AS image_path "
        "FROM ponto WHERE colaborador_id = %s ORDER BY data_hora DESC LIMIT 1"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id,))
            row = cur.fetchone()
    return dict(row) if row else None


def list_punches(limit: int = 100) -> list[dict[str, Any]]:
    sql = (
        "SELECT p.id, p.colaborador_id AS employee_id, c.nome AS name, "
        "p.data_hora AS ts, p.tipo AS type, p.confianca AS confidence, "
        "p.caminho_imagem AS image_path "
        "FROM ponto p LEFT JOIN colaborador c ON c.id = p.colaborador_id "
        "ORDER BY p.data_hora DESC LIMIT %s"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (int(limit),))
            rows = cur.fetchall()
    return [dict(r) for r in rows]


def list_punches_for(employee_id: str) -> list[dict[str, Any]]:
    sql = (
        "SELECT id, colaborador_id AS employee_id, data_hora AS ts, "
        "tipo AS type, confianca AS confidence, caminho_imagem AS image_path "
        "FROM ponto WHERE colaborador_id = %s ORDER BY data_hora ASC"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id,))
            rows = cur.fetchall()
    return [dict(r) for r in rows]


def count_punches_between(employee_id: str, start_utc_iso: str, end_utc_iso: str) -> int:
    """Conta pontos do colaborador em um intervalo UTC [start, end)."""
    sql = (
        "SELECT COUNT(*) AS c FROM ponto "
        "WHERE colaborador_id = %s AND data_hora >= %s AND data_hora < %s"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id, start_utc_iso, end_utc_iso))
            row = cur.fetchone()
    return int(row["c"]) if row else 0


def collect_punch_image_paths_before(cutoff_utc_iso: str) -> list[str]:
    """Retorna caminhos de imagem anteriores ao cutoff e zera a coluna no banco."""
    select_sql = (
        "SELECT caminho_imagem AS image_path FROM ponto "
        "WHERE caminho_imagem IS NOT NULL AND data_hora < %s"
    )
    update_sql = (
        "UPDATE ponto SET caminho_imagem = NULL "
        "WHERE caminho_imagem IS NOT NULL AND data_hora < %s"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(select_sql, (cutoff_utc_iso,))
            rows = cur.fetchall()
            paths = [r["image_path"] for r in rows if r["image_path"]]
            if paths:
                cur.execute(update_sql, (cutoff_utc_iso,))
        conn.commit()
    return paths


def delete_punches_before(cutoff_utc_iso: str) -> int:
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM ponto WHERE data_hora < %s", (cutoff_utc_iso,))
            rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


# ---------------------------------------------------------------------------
# comando_voz (voice_commands)
# ---------------------------------------------------------------------------

def add_voice_command(employee_id: str, text: str) -> int:
    now = _utc_now_iso()
    sql = (
        "INSERT INTO comando_voz (colaborador_id, texto, criado_em) "
        "VALUES (%s, %s, %s)"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id, text, now))
            new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def list_voice_commands(
    employee_id: str | None = None, limit: int = 50
) -> list[dict[str, Any]]:
    base = (
        "SELECT vc.id, vc.colaborador_id AS employee_id, c.nome AS name, "
        "vc.texto AS text, vc.criado_em AS created_at "
        "FROM comando_voz vc "
        "LEFT JOIN colaborador c ON c.id = vc.colaborador_id "
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            if employee_id:
                cur.execute(
                    base
                    + "WHERE vc.colaborador_id = %s "
                    + "ORDER BY vc.criado_em DESC LIMIT %s",
                    (employee_id, int(limit)),
                )
            else:
                cur.execute(
                    base + "ORDER BY vc.criado_em DESC LIMIT %s",
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
    sql = (
        "INSERT INTO consentimento (colaborador_id, versao, consentido_em, user_agent, ip) "
        "VALUES (%s, %s, %s, %s, %s)"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id, version, now, user_agent, ip))
            new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def revoke_consents(employee_id: str) -> int:
    now = _utc_now_iso()
    sql = (
        "UPDATE consentimento SET revogado_em = %s "
        "WHERE colaborador_id = %s AND revogado_em IS NULL"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (now, employee_id))
            rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


def latest_consent(employee_id: str) -> dict[str, Any] | None:
    sql = (
        "SELECT id, colaborador_id AS employee_id, versao AS version, "
        "consentido_em AS consented_at, revogado_em AS revoked_at, user_agent, ip "
        "FROM consentimento WHERE colaborador_id = %s "
        "ORDER BY consentido_em DESC LIMIT 1"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id,))
            row = cur.fetchone()
    return dict(row) if row else None


def list_consents(employee_id: str) -> list[dict[str, Any]]:
    sql = (
        "SELECT id, colaborador_id AS employee_id, versao AS version, "
        "consentido_em AS consented_at, revogado_em AS revoked_at, user_agent, ip "
        "FROM consentimento WHERE colaborador_id = %s ORDER BY consentido_em DESC"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id,))
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
    sql = (
        "INSERT INTO log_auditoria (data_hora, autor, acao, alvo, ip, detalhes) "
        "VALUES (%s, %s, %s, %s, %s, %s)"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (now, actor, action, target, ip, details))
            new_id = cur.lastrowid
        conn.commit()
    return int(new_id)


def list_audit(limit: int = 200) -> list[dict[str, Any]]:
    sql = (
        "SELECT id, data_hora AS ts, autor AS actor, acao AS action, "
        "alvo AS target, ip, detalhes AS details "
        "FROM log_auditoria ORDER BY data_hora DESC LIMIT %s"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (int(limit),))
            rows = cur.fetchall()
    return [dict(r) for r in rows]


def delete_audit_before(cutoff_utc_iso: str) -> int:
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM log_auditoria WHERE data_hora < %s", (cutoff_utc_iso,))
            rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


# ---------------------------------------------------------------------------
# embedding_facial (face_embeddings)
# ---------------------------------------------------------------------------

def add_face_embedding(employee_id: str, vec_bytes: bytes, dim: int, dtype: str) -> int:
    now = _utc_now_iso()
    sql = (
        "INSERT INTO embedding_facial (colaborador_id, vetor, dimensao, dtype, criado_em) "
        "VALUES (%s, %s, %s, %s, %s)"
    )
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id, vec_bytes, int(dim), dtype, now))
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
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()
    return [dict(r) for r in rows]


def count_face_embeddings(employee_id: str) -> int:
    sql = "SELECT COUNT(*) AS c FROM embedding_facial WHERE colaborador_id = %s"
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (employee_id,))
            row = cur.fetchone()
    return int(row["c"]) if row else 0


def delete_face_embeddings(employee_id: str) -> int:
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "DELETE FROM embedding_facial WHERE colaborador_id = %s",
                (employee_id,),
            )
            rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)


def clear_face_embeddings() -> int:
    with _conn_ctx() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM embedding_facial")
            rowcount = cur.rowcount
        conn.commit()
    return int(rowcount)
