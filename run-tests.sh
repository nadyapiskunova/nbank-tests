# Как запустить докер контейнер на основании докер образа
#
# 1) собрать докер образа (как компиляция для класса)
# 2) запустить докер контейнер по образу

#!/bin/bash

# Настройка
IMAGE_NAME=nbank-tests
TEST_PROFILE=${1:-api} # аргумент запуска
TIMESTAMP=$(date +"%Y%m%d_%H%M")
TEST_OUTPUT_DIR=./test-output/$TIMESTAMP

#Собираем Docker образ
echo ">>> Сборка тестов запущена"
docker build -t $IMAGE_NAME .

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
  -e APIBASEURL=http://backend:4111 \
  -e UIBASEURL=http://backend:4111 \
  -e DB_URL=jdbc:postgresql://postgres:5432/nbank \
$IMAGE_NAME

#Вывод итогов
echo ">>> Тесты завершены"
echo ">>> Лог файл: $TEST_OUTPUT_DIR/logs/run.log"
echo ">>> Результаты тестов: $TEST_OUTPUT_DIR/results"
echo ">>> Репорт: $TEST_OUTPUT_DIR/report"