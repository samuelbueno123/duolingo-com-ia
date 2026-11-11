#!/usr/bin/env bash
# ==============================================================================
# Script de Implantação Automatizada em Produção com Verificação de Saúde e Rollback
# Duolinfo Backend — Spring Boot & PostgreSQL
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
cd "$BACKEND_DIR"

echo "=== [1/5] Iniciando Verificações Pré-Deploy ==="

# Verifica se o arquivo .env ou variáveis de ambiente foram carregados
if [ -f .env ]; then
  # shellcheck disable=SC1091
  set -a && source .env && set +a
fi

# Validações de segurança obrigatórias em produção
if [ "${SPRING_PROFILES_ACTIVE:-}" = "prod" ] || [ "${SPRING_PROFILES_ACTIVE:-}" = "production" ]; then
  if [ -z "${JWT_SECRET:-}" ] || [ ${#JWT_SECRET} -lt 32 ]; then
    echo "ERRO CRÍTICO: JWT_SECRET ausente ou possui menos de 32 bytes!" >&2
    exit 1
  fi

  if [ -z "${POSTGRES_PASSWORD:-}" ] || [ "${POSTGRES_PASSWORD:-}" = "duolingo_dev_password" ]; then
    echo "ERRO CRÍTICO: POSTGRES_PASSWORD não pode ser a senha padrão de desenvolvimento!" >&2
    exit 1
  fi

  if [ -z "${APP_ADMIN_EMAILS:-}" ]; then
    echo "ERRO CRÍTICO: APP_ADMIN_EMAILS precisa estar definido!" >&2
    exit 1
  fi
fi

echo "=== [2/5] Compilando e Construindo Nova Imagem Docker ==="
IMAGE_TAG="duolingo-ia-backend:$(date +%Y%m%d%H%M%S)"
docker build -t "$IMAGE_TAG" -t "duolingo-ia-backend:latest" .

# Salva a imagem atual antes de atualizar para viabilizar rollback
PREVIOUS_IMAGE=$(docker service inspect --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' duolinfo-api 2>/dev/null || docker inspect --format '{{.Config.Image}}' duolinfo-api 2>/dev/null || echo "")

echo "=== [3/5] Aplicando Migrations do Banco e Reiniciando Serviços ==="
docker compose up -d postgres
echo "Aguardando PostgreSQL ficar saudável..."
docker compose wait postgres 2>/dev/null || sleep 5

# Reinicia a API com a nova imagem
docker compose up -d --no-deps --build api

echo "=== [4/5] Verificação de Integridade e Disponibilidade (Health Check) ==="
MAX_ATTEMPTS=24
ATTEMPT=1
HEALTHY=false

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
  echo "Tentativa $ATTEMPT/$MAX_ATTEMPTS: consultando /actuator/health..."
  
  if HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null) && [ "$HTTP_STATUS" = "200" ]; then
    echo "API respondeu HTTP 200 OK no endpoint de integridade!"
    HEALTHY=true
    break
  fi

  sleep 5
  ATTEMPT=$((ATTEMPT + 1))
done

if [ "$HEALTHY" = true ]; then
  echo "=== [5/5] Deploy concluído com sucesso! Versão ativa: $IMAGE_TAG ==="
  exit 0
else
  echo "ERRO: Health check falhou após $MAX_ATTEMPTS tentativas!" >&2
  echo "=== Disparando Rollback Imediato ==="
  if [ -n "$PREVIOUS_IMAGE" ]; then
    echo "Revertendo para imagem anterior: $PREVIOUS_IMAGE"
    API_IMAGE="$PREVIOUS_IMAGE" docker compose up -d --no-deps api
  else
    echo "Nenhuma versão anterior registrada para rollback automático. Verifique os logs."
    docker compose logs --tail=100 api
  fi
  exit 1
fi
