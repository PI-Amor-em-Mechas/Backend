#!/usr/bin/env python3
"""Remove only records marked by seed-demo-data.py; never removes usuario."""

import importlib.util
from pathlib import Path


_SEED_PATH = Path(__file__).with_name("seed-demo-data.py")
_SPEC = importlib.util.spec_from_file_location("seed_demo_data", _SEED_PATH)
if _SPEC is None or _SPEC.loader is None:
    raise RuntimeError(f"Não foi possível carregar {_SEED_PATH}")
_MODULE = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_MODULE)

TABLES = _MODULE.TABLES
connection = _MODULE.connection
delete_demo = _MODULE.delete_demo
demo_patient_ids = _MODULE.demo_patient_ids
table_columns = _MODULE.table_columns


def main() -> None:
    db = connection()
    try:
        with db.cursor() as cursor:
            schema = {table: table_columns(cursor, table) for table in TABLES}
            patient_ids = demo_patient_ids(cursor)
            if not patient_ids:
                print("Nenhum registro demo identificado; nada foi removido.")
                db.rollback()
                return
            delete_demo(cursor, schema, patient_ids)
            db.commit()
            print(f"Limpeza concluída: {len(patient_ids)} pacientes demo removidos.")
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    main()
