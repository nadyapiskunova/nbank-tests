FROM maven:3.9.9-eclipse-temurin-21

#дефолтные значения аргументов
ARG TEST_PROFILE=api
ARG APIBASEURL=http://localhost:4111
ARG UIBASEURL=http://localhost:3000

# Переменные окружения для контейнера
ENV TEST_PROFILE=${TEST_PROFILE}
ENV APIBASEURL=${APIBASEURL}
ENV UIBASEURL=${UIBASEURL}

# Работаем из папки /app
WORKDIR /app

# Копируем помник
COPY pom.xml .

# Загружаем зависимости и кешируем
RUN mvn dependency:go-offline

# Копируем весь проект
COPY . .

USER root
# mvn test -P api
# mvn -DskipTests=true surefire-report:report
# лог выводился не в консоль, а в файл

CMD /bin/bash -c " \
    mkdir -p /app/logs ; \
    { \
        echo '>>> Running tests with profile: ${TEST_PROFILE}' ; \
        mvn test -q -P ${TEST_PROFILE} ; \
        TEST_EXIT_CODE=\$? ; \
        \
        echo '>>> Running surefire-report:report' ; \
        mvn -DskipTests=true surefire-report:report || true ; \
        \
        exit \$TEST_EXIT_CODE ; \
    } > /app/logs/run.log 2>&1"