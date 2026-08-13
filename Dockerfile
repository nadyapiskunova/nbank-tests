FROM maven:3.9.9-eclipse-temurin-21

# Дефолтные значения аргументов
ARG TEST_PROFILE=api
ARG TEST_NAME=""
ARG APIBASEURL=http://localhost:4111
ARG UIBASEURL=http://localhost:3000

# Переменные окружения для контейнера
ENV TEST_PROFILE=${TEST_PROFILE}
ENV TEST_NAME=${TEST_NAME}
ENV APIBASEURL=${APIBASEURL}
ENV UIBASEURL=${UIBASEURL}

# Работаем из папки /app
WORKDIR /app

# Копируем pom.xml
COPY pom.xml .

# Загружаем зависимости и кешируем
RUN mvn dependency:go-offline

# Копируем весь проект
COPY . .

USER root

# Запуск тестов + генерация Surefire Report
# Лог пишется в файл

CMD /bin/bash -c " \
mkdir -p /app/logs ; \
{ \
echo '>>> Running tests with profile: ${TEST_PROFILE}' ; \
\
if [ -n \"${TEST_NAME}\" ]; then \
    echo '>>> Running test: ${TEST_NAME}' ; \
    mvn test -q -P \"${TEST_PROFILE}\" -Dtest=\"${TEST_NAME}\" ; \
else \
    mvn test -q -P \"${TEST_PROFILE}\" ; \
fi ; \
\
TEST_EXIT_CODE=\$? ; \
\
echo '>>> Running surefire-report:report' ; \
mvn -DskipTests=true surefire-report:report || true ; \
\
exit \$TEST_EXIT_CODE ; \
} > /app/logs/run.log 2>&1"
