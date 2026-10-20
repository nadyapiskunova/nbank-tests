#!/usr/bin/env bash

TEST_PROFILE=${1:-all}
TEST_NAME=${2:-}

ALLURE_RESULTS="target/allure-results"
ALLURE_REPORT="./allure-report"
ALLURE_HISTORY="./allure-history"

echo "========================================"
echo ">>> ALLURE TEST RUN"
echo ">>> Профиль: $TEST_PROFILE"

if [ -n "$TEST_NAME" ]; then
  echo ">>> Тест: $TEST_NAME"
else
  echo ">>> Тесты: весь профиль"
fi

echo "========================================"

# Сохраняем history предыдущего отчёта

if [ -d "$ALLURE_REPORT/history" ]; then
  echo ">>> Сохраняем историю предыдущего Allure report"

  rm -rf "$ALLURE_HISTORY"
  cp -R "$ALLURE_REPORT/history" "$ALLURE_HISTORY"
fi

# Чистим старый прогон

echo ">>> Удаляем старые результаты и отчёт"

rm -rf "$ALLURE_RESULTS" "$ALLURE_REPORT"

# Запускаем тесты и сохраняем exit code

echo ">>> Запускаем тесты..."

TEST_EXIT_CODE=0
MAVEN_LOG="target/maven-test.log"

if [ -n "$TEST_NAME" ]; then
  mvn -q clean test -P"$TEST_PROFILE" -Dtest="$TEST_NAME" \
    > "$MAVEN_LOG" 2>&1 || TEST_EXIT_CODE=$?
else
  mvn -q clean test -P"$TEST_PROFILE" \
    > "$MAVEN_LOG" 2>&1 || TEST_EXIT_CODE=$?
fi

# Добавляем предыдущую history к новому прогону

if [ -d "$ALLURE_HISTORY" ]; then
  echo ">>> Добавляем историю предыдущих прогонов"

  mkdir -p "$ALLURE_RESULTS/history"
  cp -R "$ALLURE_HISTORY"/. "$ALLURE_RESULTS/history/"
fi

# Генерируем отчёт даже если тесты упали

echo ">>> Генерируем новый Allure report"

allure generate "$ALLURE_RESULTS" \
  -o "$ALLURE_REPORT" \
  --clean

# Обновляем сохранённую history

if [ -d "$ALLURE_REPORT/history" ]; then
  rm -rf "$ALLURE_HISTORY"
  cp -R "$ALLURE_REPORT/history" "$ALLURE_HISTORY"
fi

echo ""
echo "========================================"
echo ">>> Allure report готов"
echo ">>> Результаты: $ALLURE_RESULTS"
echo ">>> Отчёт: $ALLURE_REPORT"
echo ""
echo ">>> Открыть в IDEA:"
echo "http://localhost:63342/nbank-tests/allure-report/index.html"
echo "========================================"

exit "$TEST_EXIT_CODE"
