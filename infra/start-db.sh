#!/usr/bin/env bash
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PG_BIN="/usr/lib/postgresql/16/bin"
DATA_DIR="$PROJECT_ROOT/infra/postgres-data/db"
SYMLINK_DIR="/tmp/job-assistant-pgdata"
PORT="${DB_PORT:-5433}"

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

psql -h 127.0.0.1 -p "$PORT" -U postgres -tc "SELECT 1 FROM pg_roles WHERE rolname = 'job_assistant'" | grep -q 1 || \
  psql -h 127.0.0.1 -p "$PORT" -U postgres -c "CREATE USER job_assistant WITH PASSWORD 'job_assistant_dev_pwd' CREATEDB;"

psql -h 127.0.0.1 -p "$PORT" -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'job_assistant'" | grep -q 1 || \
  psql -h 127.0.0.1 -p "$PORT" -U postgres -c "CREATE DATABASE job_assistant OWNER job_assistant;"

echo "Banco 'job_assistant' pronto na porta $PORT!"
