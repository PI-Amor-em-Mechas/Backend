"""Camada de acesso ao banco SQLite.

Alem das tabelas originais (employees, punches, voice_commands), inclui:
- consents: registro de consentimento LGPD por colaborador/versao.
- audit_log: trilha de auditoria para acoes sensiveis.

PRAGMAs aplicados:
- foreign_keys = ON
- journal_mode = WAL (melhor concorrencia leitura/escrita)
"""
from __future__ import annotations

import sqlite3
from datetime import datetime, timezone
from typing import Any

from . import config

ANONYMIZED_MARKER = "__ANON__"

def _get_conn() -> sqlite3.Connection:
    config.ensure_directories()
    conn = sqlite3.connect(config.DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")
    return conn

def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")

def _ensure_column(conn: sqlite3.Connection, table: str, column: str, decl: str) -> None:
    cols = {row["name"] for row in conn.execute(f"PRAGMA table_info({table})")}
    if column not in cols:
        conn.execute(f"ALTER TABLE {table} ADD COLUMN {column} {decl}")

def init_db() -> None:
    with _get_conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS employees (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                created_at TEXT NOT NULL,
                anonymized_at TEXT NULL
            )
            """
        )
        _ensure_column(conn, "employees", "anonymized_at", "TEXT NULL")

        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS punches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                ts TEXT NOT NULL,
                type TEXT NOT NULL,
                confidence REAL NOT NULL,
                image_path TEXT NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
            """
        )
        conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_punches_employee_ts ON punches(employee_id, ts)"
        )

        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS voice_commands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                text TEXT NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
            """
        )

        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS consents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                version TEXT NOT NULL,
                consented_at TEXT NOT NULL,
                revoked_at TEXT NULL,
                user_agent TEXT NULL,
                ip TEXT NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
            """
        )
        conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_consents_employee ON consents(employee_id)"
        )

        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts TEXT NOT NULL,
                actor TEXT NOT NULL,
                action TEXT NOT NULL,
                target TEXT NULL,
                ip TEXT NULL,
                details TEXT NULL
            )
            """
        )
        conn.execute("CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_log(ts)")

        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS face_embeddings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                vec BLOB NOT NULL,
                dim INTEGER NOT NULL,
                dtype TEXT NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
            """
        )
        conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_face_embeddings_employee "
            "ON face_embeddings(employee_id)"
        )

        conn.commit()

def add_employee(employee_id: str, name: str) -> None:
    now = _utc_now_iso()
    with _get_conn() as conn:
        conn.execute(
            """
            INSERT INTO employees (id, name, created_at)
            VALUES (?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET name = excluded.name
            """,
            (employee_id, name, now),
        )
        conn.commit()

def get_employee(employee_id: str) -> dict[str, Any] | None:
    with _get_conn() as conn:
        row = conn.execute(
            "SELECT id, name, created_at, anonymized_at FROM employees WHERE id = ?",
            (employee_id,),
        ).fetchone()
    return dict(row) if row else None

def list_employees(include_anonymized: bool = False) -> list[dict[str, Any]]:
    with _get_conn() as conn:
        if include_anonymized:
            rows = conn.execute(
                "SELECT id, name, created_at, anonymized_at FROM employees ORDER BY name"
            ).fetchall()
        else:
            rows = conn.execute(
                "SELECT id, name, created_at, anonymized_at FROM employees "
                "WHERE anonymized_at IS NULL ORDER BY name"
            ).fetchall()
    return [dict(r) for r in rows]

def delete_employee(employee_id: str) -> int:
    """Remove fisicamente o registro. Prefira `anonymize_employee` para manter
    integridade historica de pontos."""
    with _get_conn() as conn:
        cur = conn.execute("DELETE FROM employees WHERE id = ?", (employee_id,))
        conn.commit()
        return int(cur.rowcount)

def anonymize_employee(employee_id: str) -> bool:
    """Anonimiza colaborador: substitui nome por marcador, marca timestamp,
    remove comandos de voz e caminho de imagens. Mantem o historico de pontos
    para fins contabeis/legais."""
    now = _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            UPDATE employees
            SET name = ?, anonymized_at = ?
            WHERE id = ? AND anonymized_at IS NULL
            """,
            (ANONYMIZED_MARKER, now, employee_id),
        )
        conn.execute("DELETE FROM voice_commands WHERE employee_id = ?", (employee_id,))
        conn.execute(
            "UPDATE punches SET image_path = NULL WHERE employee_id = ?",
            (employee_id,),
        )
        conn.execute(
            "DELETE FROM face_embeddings WHERE employee_id = ?",
            (employee_id,),
        )
        conn.commit()
        return cur.rowcount > 0

def add_punch(
    employee_id: str,
    punch_type: str,
    confidence: float,
    image_path: str | None = None,
    ts: str | None = None,
) -> int:
    ts_value = ts or _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            INSERT INTO punches (employee_id, ts, type, confidence, image_path)
            VALUES (?, ?, ?, ?, ?)
            """,
            (employee_id, ts_value, punch_type, float(confidence), image_path),
        )
        conn.commit()
        return int(cur.lastrowid)

def get_last_punch(employee_id: str) -> dict[str, Any] | None:
    with _get_conn() as conn:
        row = conn.execute(
            """
            SELECT id, employee_id, ts, type, confidence, image_path
            FROM punches
            WHERE employee_id = ?
            ORDER BY ts DESC
            LIMIT 1
            """,
            (employee_id,),
        ).fetchone()
    return dict(row) if row else None

def list_punches(limit: int = 100) -> list[dict[str, Any]]:
    with _get_conn() as conn:
        rows = conn.execute(
            """
            SELECT p.id, p.employee_id, e.name, p.ts, p.type, p.confidence, p.image_path
            FROM punches p
            LEFT JOIN employees e ON e.id = p.employee_id
            ORDER BY p.ts DESC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
    return [dict(r) for r in rows]

def list_punches_for(employee_id: str) -> list[dict[str, Any]]:
    with _get_conn() as conn:
        rows = conn.execute(
            """
            SELECT id, employee_id, ts, type, confidence, image_path
            FROM punches
            WHERE employee_id = ?
            ORDER BY ts ASC
            """,
            (employee_id,),
        ).fetchall()
    return [dict(r) for r in rows]

def count_punches_between(employee_id: str, start_utc_iso: str, end_utc_iso: str) -> int:
    """Conta pontos do colaborador em um intervalo UTC [start, end)."""
    with _get_conn() as conn:
        row = conn.execute(
            """
            SELECT COUNT(*) AS c
            FROM punches
            WHERE employee_id = ? AND ts >= ? AND ts < ?
            """,
            (employee_id, start_utc_iso, end_utc_iso),
        ).fetchone()
    return int(row["c"]) if row else 0

def collect_punch_image_paths_before(cutoff_utc_iso: str) -> list[str]:
    """Retorna image_paths anteriores ao cutoff e zera a coluna no banco."""
    with _get_conn() as conn:
        rows = conn.execute(
            "SELECT image_path FROM punches "
            "WHERE image_path IS NOT NULL AND ts < ?",
            (cutoff_utc_iso,),
        ).fetchall()
        paths = [r["image_path"] for r in rows if r["image_path"]]
        if paths:
            conn.execute(
                "UPDATE punches SET image_path = NULL "
                "WHERE image_path IS NOT NULL AND ts < ?",
                (cutoff_utc_iso,),
            )
            conn.commit()
    return paths

def delete_punches_before(cutoff_utc_iso: str) -> int:
    with _get_conn() as conn:
        cur = conn.execute("DELETE FROM punches WHERE ts < ?", (cutoff_utc_iso,))
        conn.commit()
        return int(cur.rowcount)

def add_voice_command(employee_id: str, text: str) -> int:
    now = _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            INSERT INTO voice_commands (employee_id, text, created_at)
            VALUES (?, ?, ?)
            """,
            (employee_id, text, now),
        )
        conn.commit()
        return int(cur.lastrowid)

def list_voice_commands(
    employee_id: str | None = None, limit: int = 50
) -> list[dict[str, Any]]:
    with _get_conn() as conn:
        if employee_id:
            rows = conn.execute(
                """
                SELECT vc.id, vc.employee_id, e.name, vc.text, vc.created_at
                FROM voice_commands vc
                LEFT JOIN employees e ON e.id = vc.employee_id
                WHERE vc.employee_id = ?
                ORDER BY vc.created_at DESC
                LIMIT ?
                """,
                (employee_id, limit),
            ).fetchall()
        else:
            rows = conn.execute(
                """
                SELECT vc.id, vc.employee_id, e.name, vc.text, vc.created_at
                FROM voice_commands vc
                LEFT JOIN employees e ON e.id = vc.employee_id
                ORDER BY vc.created_at DESC
                LIMIT ?
                """,
                (limit,),
            ).fetchall()
    return [dict(r) for r in rows]

def add_consent(
    employee_id: str,
    version: str,
    user_agent: str | None = None,
    ip: str | None = None,
) -> int:
    now = _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            INSERT INTO consents (employee_id, version, consented_at, user_agent, ip)
            VALUES (?, ?, ?, ?, ?)
            """,
            (employee_id, version, now, user_agent, ip),
        )
        conn.commit()
        return int(cur.lastrowid)

def revoke_consents(employee_id: str) -> int:
    now = _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            UPDATE consents
            SET revoked_at = ?
            WHERE employee_id = ? AND revoked_at IS NULL
            """,
            (now, employee_id),
        )
        conn.commit()
        return int(cur.rowcount)

def latest_consent(employee_id: str) -> dict[str, Any] | None:
    with _get_conn() as conn:
        row = conn.execute(
            """
            SELECT id, employee_id, version, consented_at, revoked_at, user_agent, ip
            FROM consents
            WHERE employee_id = ?
            ORDER BY consented_at DESC
            LIMIT 1
            """,
            (employee_id,),
        ).fetchone()
    return dict(row) if row else None

def list_consents(employee_id: str) -> list[dict[str, Any]]:
    with _get_conn() as conn:
        rows = conn.execute(
            """
            SELECT id, employee_id, version, consented_at, revoked_at, user_agent, ip
            FROM consents
            WHERE employee_id = ?
            ORDER BY consented_at DESC
            """,
            (employee_id,),
        ).fetchall()
    return [dict(r) for r in rows]

def add_audit(
    actor: str,
    action: str,
    target: str | None = None,
    ip: str | None = None,
    details: str | None = None,
) -> int:
    now = _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            INSERT INTO audit_log (ts, actor, action, target, ip, details)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (now, actor, action, target, ip, details),
        )
        conn.commit()
        return int(cur.lastrowid)

def list_audit(limit: int = 200) -> list[dict[str, Any]]:
    with _get_conn() as conn:
        rows = conn.execute(
            "SELECT id, ts, actor, action, target, ip, details FROM audit_log "
            "ORDER BY ts DESC LIMIT ?",
            (limit,),
        ).fetchall()
    return [dict(r) for r in rows]

def delete_audit_before(cutoff_utc_iso: str) -> int:
    with _get_conn() as conn:
        cur = conn.execute("DELETE FROM audit_log WHERE ts < ?", (cutoff_utc_iso,))
        conn.commit()
        return int(cur.rowcount)

def add_face_embedding(employee_id: str, vec_bytes: bytes, dim: int, dtype: str) -> int:
    now = _utc_now_iso()
    with _get_conn() as conn:
        cur = conn.execute(
            """
            INSERT INTO face_embeddings (employee_id, vec, dim, dtype, created_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            (employee_id, vec_bytes, int(dim), dtype, now),
        )
        conn.commit()
        return int(cur.lastrowid)

def list_face_embeddings() -> list[dict[str, Any]]:
    """Retorna embeddings apenas de colaboradores nao-anonimizados."""
    with _get_conn() as conn:
        rows = conn.execute(
            """
            SELECT fe.id, fe.employee_id, fe.vec, fe.dim, fe.dtype, fe.created_at
            FROM face_embeddings fe
            INNER JOIN employees e ON e.id = fe.employee_id
            WHERE e.anonymized_at IS NULL
            """
        ).fetchall()
    return [dict(r) for r in rows]

def count_face_embeddings(employee_id: str) -> int:
    with _get_conn() as conn:
        row = conn.execute(
            "SELECT COUNT(*) AS c FROM face_embeddings WHERE employee_id = ?",
            (employee_id,),
        ).fetchone()
    return int(row["c"]) if row else 0

def delete_face_embeddings(employee_id: str) -> int:
    with _get_conn() as conn:
        cur = conn.execute(
            "DELETE FROM face_embeddings WHERE employee_id = ?",
            (employee_id,),
        )
        conn.commit()
        return int(cur.rowcount)

def clear_face_embeddings() -> int:
    with _get_conn() as conn:
        cur = conn.execute("DELETE FROM face_embeddings")
        conn.commit()
        return int(cur.rowcount)
