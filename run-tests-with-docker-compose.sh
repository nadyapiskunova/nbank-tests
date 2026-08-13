#!/usr/bin/env bash

set -euo pipefail

IMAGE_NAME="whereisnadya/nbank-tests:latest"
TEST_PROFILE="all"
COMPOSE_FILE="infra/docker_compose/docker-compose.yml"

TIMESTAMP=$(date +"%Y%m%d_%H%M")
TEST_OUTPUT_DIR="./test-output/$TIMESTAMP"

cleanup() {
  echo ">>> Останавливаем Docker Compose окружение..."
  docker compose -f "$COMPOSE_FILE" down
}

trap cleanup EXIT

echo ">>> Создаём папки для результатов..."
mkdir -p "$TEST_OUTPUT_DIR/logs"
mkdir -p "$TEST_OUTPUT_DIR/results"
mkdir -p "$TEST_OUTPUT_DIR/report"

echo ">>> Обновляем Docker image с тестами..."
docker pull "$IMAGE_NAME"

echo ">>> Поднимаем тестовое окружение..."
docker compose -f "$COMPOSE_FILE" up -d

echo ">>> Запускаем API и UI тесты..."
TEST_EXIT_CODE=0

docker run --rm \
  -v "$TEST_OUTPUT_DIR/logs":/app/logs \
  -v "$TEST_OUTPUT_DIR/results":/app/target/surefire-reports \
  -v "$TEST_OUTPUT_DIR/report":/app/target/site \
  --network nbank-network \
  -e TEST_PROFILE="$TEST_PROFILE" \
  -e APIBASEURL=http://backend:4111 \
  -e UIBASEURL=http://nginx:80 \
  -e SELENOID_URL=http://selenoid:4444 \
  -e SELENOID_UI_URL=http://selenoid-ui:8080 \
  -e DB_URL=jdbc:postgresql://postgres:5432/nbank \
  "$IMAGE_NAME" || TEST_EXIT_CODE=$?

echo ">>> Тесты завершены с кодом: $TEST_EXIT_CODE"
echo ">>> Лог: $TEST_OUTPUT_DIR/logs/run.log"
echo ">>> Результаты: $TEST_OUTPUT_DIR/results"
echo ">>> Репорт: $TEST_OUTPUT_DIR/report"

exit "$TEST_EXIT_CODE"