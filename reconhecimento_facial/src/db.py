import sqlite3
from datetime import datetime, timezone
from typing import Any

import reconhecimento_facial.src.config as config


def _get_conn() -> sqlite3.Connection:
    config.ensure_directories()
    conn = sqlite3.connect(config.DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with _get_conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS employees (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """
        )
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
        conn.commit()


def add_employee(employee_id: str, name: str) -> None:
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
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
            "SELECT id, name, created_at FROM employees WHERE id = ?",
            (employee_id,),
        ).fetchone()
    return dict(row) if row else None


def list_employees() -> list[dict[str, Any]]:
    with _get_conn() as conn:
        rows = conn.execute(
            "SELECT id, name, created_at FROM employees ORDER BY name"
        ).fetchall()
    return [dict(r) for r in rows]


def add_punch(
    employee_id: str,
    punch_type: str,
    confidence: float,
    image_path: str | None = None,
    ts: str | None = None,
) -> int:
    ts_value = ts or datetime.now(timezone.utc).isoformat(timespec="seconds")
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
