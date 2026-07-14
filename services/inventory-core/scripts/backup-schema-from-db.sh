#!/usr/bin/env bash
# Backup solo esquema desde PostgreSQL en vivo (sin filas).
# Requiere: pg_dump (brew install libpq && brew link --force libpq)
#
# Uso:
#   export SPRING_DATASOURCE_URL="jdbc:postgresql://host:5432/db"
#   export SPRING_DATASOURCE_USERNAME="postgres"
#   export SPRING_DATASOURCE_PASSWORD="***"
#   ./scripts/backup-schema-from-db.sh
#
# O con Docker local:
#   ./scripts/backup-schema-from-db.sh --docker

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/backups"
mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="$OUT_DIR/schema_from_db_${STAMP}.sql"

run_pg_dump() {
  local host="$1" port="$2" db="$3" user="$4"
  PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-${PGPASSWORD:-}}" pg_dump \
    -h "$host" -p "$port" -U "$user" -d "$db" \
    --schema-only \
    --no-owner \
    --no-privileges \
    -f "$OUT_FILE"
}

if [[ "${1:-}" == "--docker" ]]; then
  docker exec smarthome-postgres pg_dump \
    -U postgres -d smarthome_db \
    --schema-only --no-owner --no-privileges \
    > "$OUT_FILE"
else
  JDBC="${SPRING_DATASOURCE_URL:-}"
  if [[ -z "$JDBC" ]]; then
    echo "Define SPRING_DATASOURCE_URL (jdbc:postgresql://host:port/db)" >&2
    exit 1
  fi
  # jdbc:postgresql://host:5432/dbname
  REST="${JDBC#jdbc:postgresql://}"
  HOST_PORT="${REST%%/*}"
  DB="${REST#*/}"
  DB="${DB%%\?*}"
  if [[ "$HOST_PORT" == *:* ]]; then
    HOST="${HOST_PORT%%:*}"
    PORT="${HOST_PORT#*:}"
  else
    HOST="$HOST_PORT"
    PORT="5432"
  fi
  USER="${SPRING_DATASOURCE_USERNAME:-postgres}"
  run_pg_dump "$HOST" "$PORT" "$DB" "$USER"
fi

cp "$OUT_FILE" "$OUT_DIR/schema_from_db_latest.sql"
echo "Backup esquema guardado en: $OUT_FILE"
echo "Copia: $OUT_DIR/schema_from_db_latest.sql"
