#!/usr/bin/env bash
# ==============================================================================
# Script de Backup Automatizado do Banco de Dados PostgreSQL
# Duolinfo — Retenção de 14 dias com Checksum SHA256
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/backups/postgresql}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/duolingo_backup_${TIMESTAMP}.dump"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-duolingo}"
POSTGRES_DB="${POSTGRES_DB:-duolingo}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

mkdir -p "$BACKUP_DIR"

echo "=== Iniciando Backup do PostgreSQL: ${POSTGRES_DB} @ ${POSTGRES_HOST}:${POSTGRES_PORT} ==="

PGPASSWORD="${POSTGRES_PASSWORD:-duolingo_dev_password}" pg_dump \
  -h "$POSTGRES_HOST" \
  -p "$POSTGRES_PORT" \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" \
  -F c \
  -b \
  -v \
  -f "$BACKUP_FILE"

echo "Backup gravado em: $BACKUP_FILE"

# Gera checksum SHA256 para auditoria de integridade
sha256sum "$BACKUP_FILE" > "$CHECKSUM_FILE"
echo "Checksum SHA256 gerado em: $CHECKSUM_FILE"

# Aplica política de retenção removendo backups com mais de N dias
echo "Aplicando política de retenção (${RETENTION_DAYS} dias)..."
find "$BACKUP_DIR" -type f -name "duolingo_backup_*.dump*" -mtime "+$RETENTION_DAYS" -exec rm -f {} +

echo "Backup concluído com sucesso!"
