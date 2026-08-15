#!/bin/bash

# Настройка

IMAGE_NAME=nbank-tests
TEST_PROFILE=${1:-api} # аргумент запуска
TEST_NAME=${2:-}

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_TIMESTAMP=$(date +"%m%d_%H%M%S")

TEST_OUTPUT_DIR=./test-output/$TIMESTAMP


# Собираем Docker образ

echo ">>> Сборка тестов запущена"
docker build -t "$IMAGE_NAME" .

mkdir -p "$TEST_OUTPUT_DIR/logs"
mkdir -p "$TEST_OUTPUT_DIR/results"
mkdir -p "$TEST_OUTPUT_DIR/report"


# Запуск Docker контейнера

echo ">>> Тесты запущены"

docker run --rm \
  -v "$TEST_OUTPUT_DIR/logs":/app/logs \
  -v "$TEST_OUTPUT_DIR/results":/app/target/surefire-reports \
  -v "$TEST_OUTPUT_DIR/report":/app/target/site \
  --network nbank-network \
  -e TEST_PROFILE="$TEST_PROFILE" \
  -e TEST_NAME="$TEST_NAME" \
  -e APIBASEURL=http://backend:4111 \
  -e UIBASEURL=http://frontend:80 \
  -e SELENOID_URL=http://selenoid:4444 \
  -e SELENOID_UI_URL=http://selenoid-ui:8080 \
  -e DB_URL=jdbc:postgresql://postgres:5432/nbank \
  "$IMAGE_NAME"


# Переименовываем лог после завершения тестов

if [ -f "$TEST_OUTPUT_DIR/logs/run.log" ]; then
  mv "$TEST_OUTPUT_DIR/logs/run.log" \
    "$TEST_OUTPUT_DIR/logs/${LOG_TIMESTAMP}_run.log"
fi


# Вывод итогов

echo ">>> Тесты завершены"
echo ">>> Лог файл: $TEST_OUTPUT_DIR/logs/${LOG_TIMESTAMP}_run.log"
echo ">>> Результаты тестов: $TEST_OUTPUT_DIR/results"
echo ">>> Репорт: $TEST_OUTPUT_DIR/report"
