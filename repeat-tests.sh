#!/usr/bin/env bash

RUNS=$1
TEST_PROFILE=$2
TEST_NAME=${3:-}

COLLECTED_LOGS_DIR="./collected-logs"

# 1. Чистим папку от логов предыдущего эксперимента
rm -rf "$COLLECTED_LOGS_DIR"
mkdir -p "$COLLECTED_LOGS_DIR"

echo "========================================"
echo ">>> ЗАПУСК ЭКСПЕРИМЕНТА"
echo ">>> Количество прогонов: $RUNS"
echo ">>> Профиль: $TEST_PROFILE"
echo ">>> Тест: $TEST_NAME"
echo "========================================"

for ((i=1; i<=RUNS; i++)); do

  echo
  echo "========================================"
  echo ">>> ПРОГОН $i из $RUNS"
  echo "========================================"

  # Запоминаем, какие папки существовали ДО запуска
  BEFORE=$(find ./test-output -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort)

  # Запускаем тест
  if ./run-tests.sh "$TEST_PROFILE" "$TEST_NAME"; then
    RESULT="PASS"
  else
    RESULT="FAIL"
  fi

  # Смотрим, какие папки существуют ПОСЛЕ запуска
  AFTER=$(find ./test-output -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort)

  # Находим новую папку, которую создал именно этот запуск
  NEW_DIR=$(comm -13 \
    <(printf "%s\n" "$BEFORE") \
    <(printf "%s\n" "$AFTER") \
    | tail -1)

  # Берём лог именно из этой папки
  if [ -n "$NEW_DIR" ]; then
    LOG_FILE=$(find "$NEW_DIR/logs" -type f -name "*_run.log" | head -1)

    if [ -n "$LOG_FILE" ]; then
      cp "$LOG_FILE" "$COLLECTED_LOGS_DIR/"
      echo ">>> Лог сохранён: $LOG_FILE"
    else
      echo ">>> WARNING: лог прогона $i не найден"
    fi
  else
    echo ">>> WARNING: новая папка прогона $i не найдена"
  fi

  echo ">>> ПРОГОН $i: $RESULT"

done

echo
echo "========================================"
echo ">>> ЭКСПЕРИМЕНТ ЗАВЕРШЁН"
echo ">>> Прогонов: $RUNS"
echo ">>> Логи: $COLLECTED_LOGS_DIR"
echo "========================================"
