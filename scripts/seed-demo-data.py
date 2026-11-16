#!/usr/bin/env python3
"""Create a reproducible, synthetic demo dataset without changing the schema."""

from __future__ import annotations

import argparse
import base64
import os
import secrets
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any

import pymysql

try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError as exc:
    raise SystemExit(
        "Dependência ausente: instale 'cryptography' no mesmo Python que executará o seed."
    ) from exc


TABLES = (
    "paciente",
    "endereco",
    "solicitante",
    "filho",
    "dados_medicos",
    "avaliacao",
    "kit_amor",
    "kit_do_amor",
    "arquivo",
    "usuario",
)

NAMES = (
    "Mariana Oliveira Demo",
    "Carolina Martins Demo",
    "Renata Almeida Demo",
    "Juliana Ferreira Demo",
    "Patricia Souza Demo",
    "Beatriz Rodrigues Demo",
    "Camila Nogueira Demo",
    "Larissa Barbosa Demo",
    "Fernanda Costa Demo",
    "Aline Moreira Demo",
    "Gabriela Ramos Demo",
    "Isabela Teixeira Demo",
    "Nathalia Mendes Demo",
    "Luana Cardoso Demo",
    "Vanessa Araújo Demo",
    "Priscila Gomes Demo",
    "Daniela Lima Demo",
    "Leticia Martins Demo",
    "Monique Castro Demo",
    "Rafaela Pires Demo",
    "Sabrina Dias Demo",
    "Bianca Freitas Demo",
    "Amanda Moura Demo",
    "Thais Carvalho Demo",
    "Evelyn Vieira Demo",
    "Helena Monteiro Demo",
    "Mirela Neves Demo",
    "Sofia Correia Demo",
    "Clara Batista Demo",
    "Nicole Farias Demo",
)

STREETS = (
    ("Rua das Acacias Demo", "Vila Mariana", "São Paulo"),
    ("Avenida do Sol Demo", "Saúde", "São Paulo"),
    ("Rua das Orquídeas Demo", "Moema", "São Paulo"),
    ("Rua Jardim Esperança Demo", "Ipiranga", "São Paulo"),
    ("Avenida Horizonte Demo", "Tatuapé", "São Paulo"),
    ("Rua do Bosque Demo", "Lapa", "São Paulo"),
    ("Rua Estrela Guia Demo", "Santo Amaro", "São Paulo"),
    ("Avenida das Palmeiras Demo", "Penha", "São Paulo"),
    ("Rua Caminho Novo Demo", "Osasco", "Osasco"),
    ("Rua Flor de Maio Demo", "Centro", "Santo André"),
)

MOTIVOS = ("Tratamento oncológico", "Alopecia decorrente de tratamento", "Reconstrução da autoestima")
CANCERS = ("Mama", "Leucemia", "Linfoma", "Ovário", "Tireoide")
ATTENDANCE = ("SUS", "Particular", "Convênio")
MARITAL = ("Solteira", "Casada", "Divorciada", "Viúva", "União estável")
WIG_COLORS = ("Castanho escuro", "Preto", "Castanho claro", "Loiro escuro")


def connection() -> pymysql.connections.Connection:
    missing = [name for name in ("DB_PASSWORD", "PHI_ENCRYPTION_KEY") if not os.getenv(name)]
    if missing:
        raise SystemExit(f"Variáveis obrigatórias ausentes: {', '.join(missing)}")
    return pymysql.connect(
        host=os.getenv("DB_HOST", "127.0.0.1"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER", os.getenv("DB_USERNAME", "amor_app")),
        password=os.environ["DB_PASSWORD"],
        database=os.getenv("DB_NAME", "amor_em_mechas"),
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


def table_columns(cursor: Any, table: str) -> dict[str, dict[str, Any]]:
    cursor.execute(
        """
        SELECT column_name, is_nullable, column_default, extra, data_type
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = %s
        """,
        (table,),
    )
    return {row["column_name"]: row for row in cursor.fetchall()}


def quote(identifier: str) -> str:
    if not identifier.replace("_", "").isalnum():
        raise ValueError(f"Identificador inesperado: {identifier}")
    return f"`{identifier}`"


def insert_row(cursor: Any, table: str, available: dict[str, dict[str, Any]], values: dict[str, Any]) -> int:
    filtered = {key: value for key, value in values.items() if key in available}
    if not filtered:
        raise RuntimeError(f"Nenhuma coluna conhecida para inserir em {table}")
    columns = ", ".join(quote(key) for key in filtered)
    placeholders = ", ".join(["%s"] * len(filtered))
    cursor.execute(
        f"INSERT INTO {quote(table)} ({columns}) VALUES ({placeholders})",
        tuple(filtered.values()),
    )
    return int(cursor.lastrowid)


def require_columns(schema: dict[str, dict[str, dict[str, Any]]]) -> None:
    required = {
        "paciente": {"email", "nome_completo"},
        "endereco": {"rua", "bairro", "cidade"},
        "solicitante": {"nome_completo"},
        "filho": {"paciente_id", "idade"},
        "dados_medicos": {"paciente_id", "justificativa"},
        "avaliacao": {"solicitante_id"},
    }
    for table, columns in required.items():
        absent = columns - schema[table].keys()
        if absent:
            raise RuntimeError(f"Schema incompatível: {table} sem {', '.join(sorted(absent))}")


def encrypted(value: str) -> str:
    key = base64.b64decode(os.environ["PHI_ENCRYPTION_KEY"])
    if len(key) not in (16, 24, 32):
        raise RuntimeError("PHI_ENCRYPTION_KEY deve ser uma chave AES em Base64.")
    iv = secrets.token_bytes(12)
    ciphertext = AESGCM(key).encrypt(iv, value.encode("utf-8"), None)
    return base64.b64encode(iv + ciphertext).decode("ascii")


def valid_pdf() -> bytes:
    # Minimal valid PDF used only as a transparent synthetic medical-report fixture.
    return (
        b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        b"2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        b"3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 100] "
        b"/Contents 4 0 R >>\nendobj\n4 0 obj\n<< /Length 44 >>\nstream\n"
        b"BT /F1 8 Tf 10 50 Td (Relatorio DEMO sintetico) Tj ET\n"
        b"endstream\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n"
    )


def valid_png() -> bytes:
    return base64.b64decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk"
        "+A8AAQUBAScY42YAAAAASUVORK5CYII="
    )


def demo_upload_dir() -> Path:
    return Path(__file__).resolve().parents[1] / "formulario_kit_amor" / "api-para-formulario" / "uploads"


def create_demo_file(
    cursor: Any,
    schema: dict[str, dict[str, Any]],
    patient_id: int,
    filename: str,
    mime_type: str,
    content: bytes,
    tipo: str,
) -> int:
    upload_dir = demo_upload_dir()
    upload_dir.mkdir(parents=True, exist_ok=True)
    path = upload_dir / filename
    path.write_bytes(content)
    return insert_row(cursor, "arquivo", schema, {
        "nome_original": filename,
        "nome": filename,
        "mime_type": mime_type,
        "tamanho": len(content),
        "tipo": tipo,
        "caminho_arquivo": str(path.resolve()),
        "status_validacao": "PENDENTE",
        "data_upload": datetime.now(),
        "paciente_id": patient_id,
    })


def cpf(index: int) -> str:
    digits = [1, 2, 3, 4, 5, 6, 7, 8, 9, (index % 8) + 1]
    total = sum(value * (10 - position) for position, value in enumerate(digits))
    first = (total * 10) % 11
    digits.append(0 if first == 10 else first)
    total = sum(value * (11 - position) for position, value in enumerate(digits))
    second = (total * 10) % 11
    digits.append(0 if second == 10 else second)
    return "".join(map(str, digits))


def demo_patient_ids(cursor: Any) -> list[int]:
    cursor.execute(
        "SELECT id_paciente FROM paciente WHERE email LIKE %s OR nome_completo LIKE %s",
        ("%@exemplo.test", "% Demo"),
    )
    return [int(row["id_paciente"]) for row in cursor.fetchall()]


def print_schema(schema: dict[str, dict[str, dict[str, Any]]]) -> None:
    print("Schema verificado:")
    for table in TABLES:
        columns = schema[table]
        required = sum(
            1 for column in columns.values()
            if column["is_nullable"] == "NO" and column["column_default"] is None
            and "auto_increment" not in column["extra"]
        )
        print(f"  {table}: {len(columns)} colunas, {required} obrigatórias")


def seed(reset: bool) -> None:
    db = connection()
    try:
        with db.cursor() as cursor:
            schema = {table: table_columns(cursor, table) for table in TABLES}
            missing_tables = [table for table, columns in schema.items() if not columns]
            if missing_tables:
                raise RuntimeError(f"Tabelas ausentes: {', '.join(missing_tables)}")
            require_columns(schema)
            print_schema(schema)
            existing = demo_patient_ids(cursor)
            if existing and not reset:
                print(f"Massa demo já existe ({len(existing)} pacientes); nenhuma duplicação criada.")
                db.rollback()
                return
            if reset and existing:
                delete_demo(cursor, schema, existing)

            address_ids = []
            for index in range(30):
                street, district, city = STREETS[index % len(STREETS)]
                address_ids.append(insert_row(cursor, "endereco", schema["endereco"], {
                    "rua": street, "numero": str(40 + index), "bairro": district,
                    "cidade": city, "estado": "SP", "cep": f"0{1000 + index:03d}-900",
                    "complemento": ("Apto " + str(10 + index)) if index % 3 == 0 else (
                        "Bloco B" if index % 3 == 1 else "Casa 2"
                    ),
                }))

            requester_ids = []
            for index in range(25):
                requester_ids.append(insert_row(cursor, "solicitante", schema["solicitante"], {
                    "nome_completo": f"Responsável {NAMES[index].replace(' Demo', '')} Demo",
                    "rg": f"90.{index:03d}.700-Demo",
                }))

            patient_ids = []
            for index, name in enumerate(NAMES):
                children = index % 4
                requested = date.today() - timedelta(days=8 + index * 6)
                birth = date(1970 + (index % 28), (index % 12) + 1, (index % 26) + 1)
                status = "ATIVA" if index < 15 else ("CONCLUIDA" if index < 25 else "CANCELADA")
                patient_ids.append(insert_row(cursor, "paciente", schema["paciente"], {
                    "data_pedido": requested, "dt_pedido": requested, "email": f"demo.paciente{index + 1:02d}@exemplo.test",
                    "nome_completo": name, "celular": f"(11) 9887{index:04d}", "cel": f"(11) 9887{index:04d}",
                    "data_nascimento": birth, "dt_nasc": birth, "estado_civil": MARITAL[index % len(MARITAL)],
                    "qtd_filhos": children, "qtd_pessoas_casa": 1 + (index % 6), "qtd_pessoas_em_casa": 1 + (index % 6),
                    "tem_filhos": children > 0, "cpf": encrypted(cpf(index)),
                    "cabelo_antes": b"", "consentimento_lgpd": True, "dt_consentimento": datetime.now(),
                    "finalidade_tratamento": "Atendimento demo para demonstração do fluxo",
                    "dados_anonimizados": False, "status_solicitacao": status,
                    "endereco_id": address_ids[index], "solicitante_id": requester_ids[index % len(requester_ids)],
                }))
                for child_index in range(children):
                    insert_row(cursor, "filho", schema["filho"], {
                        "paciente_id": patient_ids[-1], "idade": 2 + ((index + child_index * 4) % 16),
                    })

            medical_ids = []
            for index in range(27):
                patient_id = patient_ids[index]
                medical_ids.append(insert_row(cursor, "dados_medicos", schema["dados_medicos"], {
                    "motivo": encrypted(MOTIVOS[index % len(MOTIVOS)]),
                    "tipo_cancer": encrypted(CANCERS[index % len(CANCERS)]),
                    "justificativa": encrypted("Registro sintético para demonstração do atendimento."),
                    "inicio_tratamento": date.today() - timedelta(days=90 + index * 3),
                    "dt_inicio_tratamento": date.today() - timedelta(days=90 + index * 3),
                    "tipo_atendimento": ATTENDANCE[index % len(ATTENDANCE)],
                    "relatorio_medico": valid_pdf(), "paciente_id": patient_id,
                }))
                if "dados_medicos_id" in schema["paciente"]:
                    cursor.execute("UPDATE paciente SET dados_medicos_id = %s WHERE id_paciente = %s",
                                   (medical_ids[-1], patient_id))

            if schema["arquivo"]:
                fixtures = (
                    (0, "laudo-medico-demo-pendente-a.pdf", "application/pdf", valid_pdf(), "RELATORIO_MEDICO"),
                    (0, "foto-cabelo-demo-a.png", "image/png", valid_png(), "FOTO_CABELO"),
                    (1, "laudo-medico-demo-pendente-b.pdf", "application/pdf", valid_pdf(), "RELATORIO_MEDICO"),
                    (1, "foto-cabelo-demo-b.png", "image/png", valid_png(), "FOTO_CABELO"),
                )
                for patient_index, filename, mime_type, content, tipo in fixtures:
                    file_id = create_demo_file(
                        cursor, schema["arquivo"], patient_ids[patient_index],
                        filename, mime_type, content, tipo,
                    )
                    if tipo == "FOTO_CABELO" and "cabelo_antes_id" in schema["paciente"]:
                        cursor.execute(
                            "UPDATE paciente SET cabelo_antes_id = %s WHERE id_paciente = %s",
                            (file_id, patient_ids[patient_index]),
                        )

            for index in range(18):
                insert_row(cursor, "avaliacao", schema["avaliacao"], {
                    "nota_avaliacao": date.today() - timedelta(days=index),
                    "concentimento": True, "consentimento": True, "concluido": index % 3 != 0,
                    "data_conclusao": datetime.now(), "dt_conclusao": date.today(),
                    "nota_formulario": 7 + (index % 4), "solicitante_id": requester_ids[index],
                })

            kits = 0
            if schema["kit_amor"]:
                for index in range(16):
                    insert_row(cursor, "kit_amor", schema["kit_amor"], {
                        "cor_peruca": WIG_COLORS[index % len(WIG_COLORS)],
                        "paciente_id": patient_ids[index], "solicitante_id": requester_ids[index],
                    })
                    kits += 1
            db.commit()
            counts = {table: count_demo(cursor, table) for table in (
                "paciente", "endereco", "solicitante", "filho", "dados_medicos", "avaliacao", "kit_amor",
            )}
            print("Seed concluído:")
            print("  " + "\n  ".join(f"{key}={value}" for key, value in counts.items()))
            print(f"  kits_criados_nesta_execucao={kits}")
            print("  arquivos=0 (nenhum blob biométrico ou documento aleatório inserido)")
            validate(cursor, schema)
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def count_demo(cursor: Any, table: str) -> int:
    if table in ("paciente",):
        cursor.execute(f"SELECT COUNT(*) AS n FROM {quote(table)} WHERE email LIKE %s OR nome_completo LIKE %s",
                       ("%@exemplo.test", "% Demo"))
    elif table == "solicitante":
        cursor.execute(f"SELECT COUNT(*) AS n FROM {quote(table)} WHERE nome_completo LIKE %s", ("% Demo",))
    else:
        cursor.execute(f"SELECT COUNT(*) AS n FROM {quote(table)}")
    return int(cursor.fetchone()["n"])


def validate(cursor: Any, schema: dict[str, dict[str, dict[str, Any]]]) -> None:
    cursor.execute("""
        SELECT COUNT(*) AS n FROM paciente p
        LEFT JOIN endereco e ON e.id_endereco = p.endereco_id
        WHERE (p.email LIKE %s OR p.nome_completo LIKE %s) AND e.id_endereco IS NULL
    """, ("%@exemplo.test", "% Demo"))
    orphan_addresses = int(cursor.fetchone()["n"])
    cursor.execute("SELECT COUNT(*) AS n FROM paciente WHERE email LIKE %s", ("%@exemplo.test",))
    demo_patients = int(cursor.fetchone()["n"])
    cursor.execute("SELECT COUNT(*) AS n FROM paciente WHERE email LIKE %s GROUP BY email HAVING COUNT(*) > 1",
                   ("%@exemplo.test",))
    duplicate_emails = cursor.rowcount
    cursor.execute("SELECT COUNT(*) AS n FROM paciente WHERE status_solicitacao NOT IN ('ATIVA','CONCLUIDA','CANCELADA')",
                   ())
    invalid_status = int(cursor.fetchone()["n"]) if "status_solicitacao" in schema["paciente"] else 0
    print(f"Integridade: pacientes_sem_endereco={orphan_addresses}, emails_duplicados={duplicate_emails}, "
          f"status_invalidos={invalid_status}, pacientes_demo={demo_patients}")
    if orphan_addresses or duplicate_emails or invalid_status:
        raise RuntimeError("A validação de integridade da massa demo falhou.")


def delete_demo(cursor: Any, schema: dict[str, dict[str, dict[str, Any]]], patient_ids: list[int]) -> None:
    if not patient_ids:
        return
    marks = ", ".join(["%s"] * len(patient_ids))
    cursor.execute(f"SELECT DISTINCT solicitante_id FROM paciente WHERE id_paciente IN ({marks}) AND solicitante_id IS NOT NULL",
                   tuple(patient_ids))
    requester_ids = [int(row["solicitante_id"]) for row in cursor.fetchall()]
    if schema["kit_amor"]:
        cursor.execute(f"DELETE FROM kit_amor WHERE paciente_id IN ({marks})", tuple(patient_ids))
    if requester_ids and schema["kit_do_amor"]:
        rmarks = ", ".join(["%s"] * len(requester_ids))
        cursor.execute(f"DELETE FROM kit_do_amor WHERE solicitante_id IN ({rmarks})", tuple(requester_ids))
    if requester_ids and schema["avaliacao"]:
        rmarks = ", ".join(["%s"] * len(requester_ids))
        cursor.execute(f"DELETE FROM avaliacao WHERE solicitante_id IN ({rmarks})", tuple(requester_ids))
    if schema["dados_medicos"]:
        cursor.execute(f"DELETE FROM dados_medicos WHERE paciente_id IN ({marks})", tuple(patient_ids))
    if schema["arquivo"] and "paciente_id" in schema["arquivo"]:
        cursor.execute(
            f"SELECT caminho_arquivo FROM arquivo WHERE paciente_id IN ({marks})",
            tuple(patient_ids),
        )
        for row in cursor.fetchall():
            path = row.get("caminho_arquivo")
            if path:
                try:
                    os.remove(path)
                except FileNotFoundError:
                    pass
        cursor.execute(f"DELETE FROM arquivo WHERE paciente_id IN ({marks})", tuple(patient_ids))
    cursor.execute(f"DELETE FROM filho WHERE paciente_id IN ({marks})", tuple(patient_ids))
    cursor.execute(f"SELECT DISTINCT endereco_id FROM paciente WHERE id_paciente IN ({marks}) AND endereco_id IS NOT NULL",
                   tuple(patient_ids))
    address_ids = [int(row["endereco_id"]) for row in cursor.fetchall()]
    cursor.execute(f"DELETE FROM paciente WHERE id_paciente IN ({marks})", tuple(patient_ids))
    if address_ids:
        amarks = ", ".join(["%s"] * len(address_ids))
        cursor.execute(f"DELETE FROM endereco WHERE id_endereco IN ({amarks})", tuple(address_ids))
    if requester_ids:
        rmarks = ", ".join(["%s"] * len(requester_ids))
        cursor.execute(
            f"DELETE FROM solicitante WHERE id_solicitante IN ({rmarks}) "
            "AND NOT EXISTS (SELECT 1 FROM paciente WHERE paciente.solicitante_id = solicitante.id_solicitante)",
            tuple(requester_ids),
        )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reset-demo", action="store_true", help="remove e recria somente a massa identificada como demo")
    args = parser.parse_args()
    seed(args.reset_demo)


if __name__ == "__main__":
    main()
