#!/usr/bin/env python3
"""Genera un SQL solo con estructura (DDL) a partir de migraciones Flyway, sin datos."""

from __future__ import annotations

import re
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIGRATION_DIR = ROOT / "src/main/resources/db/migration"
OUT_DIR = ROOT / "backups"

SKIP_PREFIX = re.compile(
    r"^\s*(INSERT|UPDATE|DELETE|TRUNCATE)\b",
    re.IGNORECASE,
)
SKIP_SETVAL = re.compile(r"^\s*SELECT\s+setval\s*\(", re.IGNORECASE)
VERSION = re.compile(r"^V(\d+)__")


def migration_order(path: Path) -> int:
    match = VERSION.match(path.name)
    if not match:
        return 9999
    return int(match.group(1))


def split_statements(sql: str) -> list[str]:
    statements: list[str] = []
    current: list[str] = []
    for line in sql.splitlines():
        current.append(line)
        if line.rstrip().endswith(";"):
            statements.append("\n".join(current).strip())
            current = []
    if current:
        tail = "\n".join(current).strip()
        if tail:
            statements.append(tail)
    return statements


def first_sql_line(stmt: str) -> str:
    for line in stmt.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("--"):
            continue
        return stripped
    return ""


def is_data_statement(stmt: str) -> bool:
    first = first_sql_line(stmt)
    return bool(SKIP_PREFIX.match(first) or SKIP_SETVAL.match(first))


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    files = sorted(MIGRATION_DIR.glob("V*.sql"), key=migration_order)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    out_path = OUT_DIR / f"schema_only_{timestamp}.sql"

    header = [
        "-- SmartInventory — backup de esquema (solo tablas/índices, sin datos)",
        f"-- Generado: {datetime.now(timezone.utc).isoformat()}",
        "-- Fuente: migraciones Flyway (INSERT/UPDATE/DELETE omitidos)",
        "",
        "BEGIN;",
        "",
    ]

    body: list[str] = []
    skipped = 0

    for file in files:
        body.append(f"-- === {file.name} ===")
        body.append("")
        for stmt in split_statements(file.read_text(encoding="utf-8")):
            if is_data_statement(stmt):
                skipped += 1
                continue
            body.append(stmt)
            if not stmt.endswith(";"):
                body.append(";")
            body.append("")

    footer = [
        "COMMIT;",
        "",
        f"-- Sentencias de datos omitidas: {skipped}",
    ]

    out_path.write_text("\n".join(header + body + footer), encoding="utf-8")
    latest = OUT_DIR / "schema_only_latest.sql"
    latest.write_text(out_path.read_text(encoding="utf-8"), encoding="utf-8")

    print(f"OK: {out_path}")
    print(f"OK: {latest} (copia fija)")
    print(f"Sentencias de datos omitidas: {skipped}")


if __name__ == "__main__":
    main()
