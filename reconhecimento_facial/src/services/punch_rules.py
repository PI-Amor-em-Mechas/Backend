"""Regras de ponto (IN/OUT) e cooldown.

Regra IN/OUT automatica:
- Conta quantos pontos o colaborador ja registrou **no dia atual** (fuso horario
  local definido em `config.LOCAL_TIMEZONE`).
- Contagem par (0, 2, 4, ...) -> proximo ponto eh `IN` (entrada).
- Contagem impar (1, 3, 5, ...) -> proximo ponto eh `OUT` (saida).

Essa regra evita o comportamento anterior, que alternava apenas com base no
ultimo ponto e podia "travar" em IN/OUT caso o colaborador esquecesse de bater
o par do dia anterior: um novo dia sempre comeca com IN.
"""
from __future__ import annotations

from datetime import datetime, time, timedelta, timezone

from .. import config
from .. import db

try:
    from zoneinfo import ZoneInfo  # type: ignore
except ImportError:  # pragma: no cover
    ZoneInfo = None  # type: ignore

def _local_tz() -> timezone:
    if ZoneInfo is not None:
        try:
            return ZoneInfo(config.LOCAL_TIMEZONE)  # type: ignore[return-value]
        except Exception:
            return timezone.utc
    return timezone.utc

def _today_utc_bounds() -> tuple[str, str]:
    """Retorna [inicio, fim) do dia local atual convertidos para UTC ISO."""
    tz = _local_tz()
    now_local = datetime.now(tz)
    start_local = datetime.combine(now_local.date(), time.min, tzinfo=tz)
    end_local = start_local + timedelta(days=1)
    start_utc = start_local.astimezone(timezone.utc).isoformat(timespec="seconds")
    end_utc = end_local.astimezone(timezone.utc).isoformat(timespec="seconds")
    return start_utc, end_utc

def punches_today(employee_id: str) -> int:
    start_utc, end_utc = _today_utc_bounds()
    return db.count_punches_between(employee_id, start_utc, end_utc)

def determine_punch_type(employee_id: str) -> str:
    """Determina IN/OUT com base na contagem de pontos do dia atual."""
    count = punches_today(employee_id)
    return "IN" if count % 2 == 0 else "OUT"

def _parse_ts(ts_text: str) -> datetime:
    dt = datetime.fromisoformat(ts_text)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt

def is_within_cooldown(employee_id: str) -> bool:
    last = db.get_last_punch(employee_id)
    if not last:
        return False
    now = datetime.now(timezone.utc)
    delta = (now - _parse_ts(last["ts"])).total_seconds()
    return delta < config.PUNCH_DUPLICATE_WINDOW_SECONDS
