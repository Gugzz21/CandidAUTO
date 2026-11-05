#!/usr/bin/env bash
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -f "$PROJECT_ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$PROJECT_ROOT/.env"
  set +a
fi

PG_BIN="/usr/lib/postgresql/16/bin"
DATA_DIR="$PROJECT_ROOT/infra/postgres-data/db"
SYMLINK_DIR="/tmp/job-assistant-pgdata"
PORT="${DB_PORT:-5433}"
DB_TARGET_NAME="${DB_NAME:-job_assistant}"
DB_TARGET_USER="${DB_USER:-job_assistant}"
DB_TARGET_PWD="${DB_PASSWORD:-}"

mkdir -p "$PROJECT_ROOT/infra/postgres-data"
ln -sfn "$DATA_DIR" "$SYMLINK_DIR"

if [ ! -f "$DATA_DIR/PG_VERSION" ]; then
  "$PG_BIN/initdb" -D "$SYMLINK_DIR" -U postgres --auth=trust
fi

if "$PG_BIN/pg_isready" -h 127.0.0.1 -p "$PORT" >/dev/null 2>&1; then
  echo "PostgreSQL já está rodando em 127.0.0.1:$PORT"
else
  "$PG_BIN/pg_ctl" -D "$SYMLINK_DIR" -l /tmp/job-assistant-postgres.log -o "-c listen_addresses=127.0.0.1 -k /tmp -p $PORT" start
  echo "PostgreSQL iniciado em 127.0.0.1:$PORT"
fi

if ! psql -h 127.0.0.1 -p "$PORT" -U postgres -tc "SELECT 1 FROM pg_roles WHERE rolname = '$DB_TARGET_USER'" | grep -q 1; then
  if [ -n "$DB_TARGET_PWD" ]; then
    psql -h 127.0.0.1 -p "$PORT" -U postgres -c "CREATE USER $DB_TARGET_USER WITH PASSWORD '$DB_TARGET_PWD' CREATEDB;"
  else
    psql -h 127.0.0.1 -p "$PORT" -U postgres -c "CREATE USER $DB_TARGET_USER CREATEDB;"
  fi
fi

psql -h 127.0.0.1 -p "$PORT" -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_TARGET_NAME'" | grep -q 1 || \
  psql -h 127.0.0.1 -p "$PORT" -U postgres -c "CREATE DATABASE $DB_TARGET_NAME OWNER $DB_TARGET_USER;"

echo "Banco '$DB_TARGET_NAME' pronto na porta $PORT!"
