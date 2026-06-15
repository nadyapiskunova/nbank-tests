package iteration_2;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AccountsTransferTest extends BaseTest {
    public static Stream<Arguments> validDataForUserCanTransferBetweenTheirAccountWithValidDataTest() {

        return Stream.of(
                Arguments.of(0.01),
                Arguments.of(9999.99),
                Arguments.of(10000.00)
        );
    }

    @MethodSource("validDataForUserCanTransferBetweenTheirAccountWithValidDataTest")
    @ParameterizedTest
    public void userCanTransferBetweenTheirAccountWithValidDataTest(Double amount) {
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт(счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // пользователь делает депозит еще раз, так как ограничение в 5000
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяем что депозит есть
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10000.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(2))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(5000.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // пользователь делает трансфер с firstAccountId на secondAccountId
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %s
                        }
                        """.formatted(firstAccountId, secondAccountId, amount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("message", Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.equalTo(amount.floatValue()))
                .body("receiverAccountId", Matchers.equalTo(secondAccountId))
                .body("senderAccountId", Matchers.equalTo(firstAccountId));

        // проверяю записи счетов
        BigDecimal expectedSenderBalance =
                BigDecimal.valueOf(10000.00)
                        .subtract(BigDecimal.valueOf(amount));

        BigDecimal expectedReceiverBalance =
                BigDecimal.valueOf(amount);

        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body(
                        "find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(expectedSenderBalance.floatValue())
                )
                .body(
                        "find { it.id == %d }.balance".formatted(secondAccountId),
                        Matchers.equalTo(expectedReceiverBalance.floatValue())
                )
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.hasItem("TRANSFER_OUT"))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.hasItem(amount.floatValue()))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.hasItem(secondAccountId))
                .body("find { it.id == %d }.transactions.type".formatted(secondAccountId),
                        Matchers.hasItem("TRANSFER_IN"))
                .body("find { it.id == %d }.transactions.amount".formatted(secondAccountId),
                        Matchers.hasItem(amount.floatValue()))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(secondAccountId),
                        Matchers.hasItem(firstAccountId));
    }

    public static Stream<Arguments> validDataForCanTransferToExternalAccountWithValidDataTest() {

        return Stream.of(
                Arguments.of(0.01),
                Arguments.of(9999.99),
                Arguments.of(10000.00)
        );
    }

    @MethodSource("validDataForCanTransferToExternalAccountWithValidDataTest")
    @ParameterizedTest
    public void userCanTransferToExternalAccountWithValidDataTest(Double amount) {
        // создание пользователя 1
        Integer firstUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(firstUserId);

        // создание пользователя 2
        Integer secondUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(secondUserId);

        // получаем токен юзера 1
        String firstUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // получаем токен юзера 2
        String secondUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт(счет) для юзера 1
        Integer accountIdByFirstUser = given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт(счет) для юзера 2
        Integer accountIdBySecondUser = given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера1 есть
        given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdByFirstUser));

        // проверяем что аккаунт у юзера2 есть
        given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdBySecondUser));


        // пользователь 1 делает депозит на свой счет
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
        // пользователь 1 делает депозит на свой счет еще раз
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].id", Matchers.equalTo(accountIdByFirstUser))
                .body("[0].balance", Matchers.equalTo(10000.0f))
                .body("[0].transactions", Matchers.hasSize(2))
                .body("[0].transactions[0].amount", Matchers.equalTo(5000.0f))
                .body("[0].transactions[1].amount", Matchers.equalTo(5000.0f))
                .body("[0].transactions[0].type", Matchers.equalTo("DEPOSIT"));

        // юзер 1 делает трансфер юзеру 2
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %s
                        }
                        """.formatted(accountIdByFirstUser, accountIdBySecondUser, amount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("message", Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.equalTo(amount.floatValue()))
                .body("receiverAccountId", Matchers.equalTo(accountIdBySecondUser))
                .body("senderAccountId", Matchers.equalTo(accountIdByFirstUser));

        // проверяю записи счетов
        BigDecimal expectedSenderBalance =
                BigDecimal.valueOf(10000.00)
                        .subtract(BigDecimal.valueOf(amount));

        BigDecimal expectedReceiverBalance =
                BigDecimal.valueOf(amount);

        // проверяем счет юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(expectedSenderBalance.floatValue()))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.hasItem("TRANSFER_OUT"))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.hasItem(amount.floatValue()))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.hasItem(accountIdBySecondUser));

        // проверяем счет юзера 2
        given()
                .header("Authorization", secondUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdBySecondUser))
                .body("find { it.id == %d }.balance".formatted(accountIdBySecondUser),
                        Matchers.equalTo(expectedReceiverBalance.floatValue()))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdBySecondUser),
                        Matchers.hasItem("TRANSFER_IN"))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdBySecondUser),
                        Matchers.hasItem(amount.floatValue()))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdBySecondUser),
                        Matchers.hasItem(accountIdByFirstUser));
    }

    public static Stream<Arguments> invalidDataForUserCannotTransferBetweenTheirAccountWithInvalidDataTest() {

        return Stream.of(
                Arguments.of(-1.00, "Transfer amount must be at least 0.01"),
                Arguments.of(0.00, "Transfer amount must be at least 0.01"),
                Arguments.of(10000.01, "Transfer amount cannot exceed 10000")
        );
    }

    @MethodSource("invalidDataForUserCannotTransferBetweenTheirAccountWithInvalidDataTest")
    @ParameterizedTest
    public void userCannotTransferBetweenTheirAccountWithInvalidDataTest(Double amount, String errorValue) {
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт(счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // пользователь делает депозит еще раз, так как ограничение в 5000
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
        // пользователь делает депозит еще раз, так как ограничение в 5000
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяем что депозит есть
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(15000.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(3))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(5000.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // пользователь делает трансфер с firstAccountId на secondAccountId
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %s
                        }
                        """.formatted(firstAccountId, secondAccountId, amount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));

        // проверяю, что трансфера не было
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))

                // счет отправителя не изменился
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(15000.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(3))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(5000.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)))

                // у отправителя нет операции списания
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")))

                // счет получателя не изменился
                .body("find { it.id == %d }.balance".formatted(secondAccountId),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(secondAccountId),
                        Matchers.hasSize(0))

                // у получателя нет операции зачисления
                .body("find { it.id == %d }.transactions.type".formatted(secondAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }

    public static Stream<Arguments> invalidDataForUserCannotTransferToExternalAccountWithInvalidDataTest() {

        return Stream.of(
                Arguments.of(-1.00, "Transfer amount must be at least 0.01"),
                Arguments.of(0.00, "Transfer amount must be at least 0.01"),
                Arguments.of(10000.01, "Transfer amount cannot exceed 10000")
        );
    }

    @MethodSource("invalidDataForUserCannotTransferToExternalAccountWithInvalidDataTest")
    @ParameterizedTest
    public void userCannotTransferToExternalAccountWithInvalidDataTest(Double amount, String errorValue) {
        // создание пользователя 1
        Integer firstUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(firstUserId);

        // создание пользователя 2
        Integer secondUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(secondUserId);

        // получаем токен юзера 1
        String firstUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // получаем токен юзера 2
        String secondUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт(счет) для юзера 1
        Integer accountIdByFirstUser = given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт(счет) для юзера 2
        Integer accountIdBySecondUser = given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера1 есть
        given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdByFirstUser));

        // проверяем что аккаунт у юзера2 есть
        given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdBySecondUser));


        // пользователь 1 делает депозит на свой счет
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
        // пользователь 1 делает депозит на свой счет еще раз
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // пользователь 1 делает депозит на свой счет еще раз
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 5000
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяю, что депозит есть у юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(15000.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdByFirstUser),
                        Matchers.hasSize(3))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(5000.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(accountIdByFirstUser)));

        // юзер 1 делает трансфер юзеру 2
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %s
                        }
                        """.formatted(accountIdByFirstUser, accountIdBySecondUser, amount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));


        // проверяю, что трансфера не было

        // проверяем счет юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(15000.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdByFirstUser),
                        Matchers.hasSize(3))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(5000.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(accountIdByFirstUser)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")));

        // проверяем счет юзера 2
        given()
                .header("Authorization", secondUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdBySecondUser))
                .body("find { it.id == %d }.balance".formatted(accountIdBySecondUser),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdBySecondUser),
                        Matchers.hasSize(0))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdBySecondUser),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }

    @Test
    public void userCannotTransferAmountExceedingBalanceBetweenTheirAccountTest(){
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт1 (счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт2 (счет)
        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяю наличие депозита
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // пользователь делает трансфер с firstAccountId на secondAccountId
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 15
                        }
                        """.formatted(firstAccountId, secondAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));

        // проверяю, что трансфера не было
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))

                // счет отправителя не изменился
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)))

                // у отправителя нет операции списания
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")))

                // счет получателя не изменился
                .body("find { it.id == %d }.balance".formatted(secondAccountId),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(secondAccountId),
                        Matchers.hasSize(0))

                // у получателя нет операции зачисления
                .body("find { it.id == %d }.transactions.type".formatted(secondAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }

    @Test
    public void userCannotTransferAmountExceedingBalanceToExternalAccountTest(){
        // создание пользователя 1
        Integer firstUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(firstUserId);

        // создание пользователя 2
        Integer secondUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(secondUserId);

        // получаем токен юзера 1
        String firstUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // получаем токен юзера 2
        String secondUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт(счет) для юзера 1
        Integer accountIdByFirstUser = given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт(счет) для юзера 2
        Integer accountIdBySecondUser = given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера1 есть
        given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdByFirstUser));

        // проверяем что аккаунт у юзера2 есть
        given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdBySecondUser));


        // пользователь 1 делает депозит на свой счет
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяю, что депозит есть у юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdByFirstUser),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(accountIdByFirstUser)));

        // юзер 1 делает трансфер юзеру 2
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 15
                        }
                        """.formatted(accountIdByFirstUser, accountIdBySecondUser))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));

        // проверяю, что трансфера не было

        // проверяем счет юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdByFirstUser),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(accountIdByFirstUser)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")));

        // проверяем счет юзера 2
        given()
                .header("Authorization", secondUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdBySecondUser))
                .body("find { it.id == %d }.balance".formatted(accountIdBySecondUser),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdBySecondUser),
                        Matchers.hasSize(0))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdBySecondUser),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }

    @Test
    public void adminCannotTransferBetweenUserAccountsTest(){
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем первый аккаунт(счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем второй аккаунт(счет)
        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяю наличие депозита
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // админ делает трансфер между аккаунтами пользователя: с firstAccountId на secondAccountId
        given()
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 10
                        }
                        """.formatted(firstAccountId, secondAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // проверяю, что трансфера не было
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))

                // счет отправителя не изменился
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)))

                // у отправителя нет операции списания
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")))

                // счет получателя не изменился
                .body("find { it.id == %d }.balance".formatted(secondAccountId),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(secondAccountId),
                        Matchers.hasSize(0))

                // у получателя нет операции зачисления
                .body("find { it.id == %d }.transactions.type".formatted(secondAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));

    }

    @Test
    public void adminCannotTransferFromUserAccountToAnotherUsersAccountTest(){
        // создание пользователя 1
        Integer firstUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(firstUserId);

        // создание пользователя 2
        Integer secondUserId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(secondUserId);

        // получаем токен юзера 1
        String firstUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // получаем токен юзера 2
        String secondUserAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(secondUsername, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт(счет) для юзера 1
        Integer accountIdByFirstUser = given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт(счет) для юзера 2
        Integer accountIdBySecondUser = given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера1 есть
        given()
                .header("Authorization", firstUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdByFirstUser));

        // проверяем что аккаунт у юзера2 есть
        given()
                .header("Authorization", secondUserAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountIdBySecondUser));


        // пользователь 1 делает депозит на свой счет
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(accountIdByFirstUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяю, что депозит есть у юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdByFirstUser),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(accountIdByFirstUser)));

        // админ делает трансфер со счета юзера1 на счет юзеру2
        given()
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 10
                        }
                        """.formatted(accountIdByFirstUser, accountIdBySecondUser))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // проверяю, что трансфера не было

        // проверяем счет юзера 1
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdByFirstUser))
                .body("find { it.id == %d }.balance".formatted(accountIdByFirstUser),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdByFirstUser),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(accountIdByFirstUser),
                        Matchers.everyItem(Matchers.equalTo(accountIdByFirstUser)))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdByFirstUser),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")));

        // проверяем счет юзера 2
        given()
                .header("Authorization", secondUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItem(accountIdBySecondUser))
                .body("find { it.id == %d }.balance".formatted(accountIdBySecondUser),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(accountIdBySecondUser),
                        Matchers.hasSize(0))
                .body("find { it.id == %d }.transactions.type".formatted(accountIdBySecondUser),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }

    @Test
    public void unauthenticatedUserCannotTransferBetweenOwnAccountsTest(){
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт1 (счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт2 (счет)
        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяем наличие депозита
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // пользователь делает трансфер с firstAccountId на secondAccountId
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 10
                        }
                        """.formatted(firstAccountId, secondAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        // проверяю, что трансфера не было
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))

                // счет отправителя не изменился
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)))

                // у отправителя нет операции списания
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")))

                // счет получателя не изменился
                .body("find { it.id == %d }.balance".formatted(secondAccountId),
                        Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(secondAccountId),
                        Matchers.hasSize(0))

                // у получателя нет операции зачисления
                .body("find { it.id == %d }.transactions.type".formatted(secondAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }

    @Test
    public void userCannotTransferToNonExistentAccountTest(){
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт1 (счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт2 (счет)
        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяем наличие депозита
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // пользователь делает трансфер с firstAccountId на nonExistingAccountId
        Integer nonExistingAccountId = Integer.MAX_VALUE;
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 10
                        }
                        """.formatted(firstAccountId, nonExistingAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));

        // проверяю, что трансфера не было
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))

                // счет отправителя не изменился
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)))

                // у отправителя нет операции списания
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")));
    }

    @Test
    public void userCannotTransferFromNonExistentAccountTest(){
        // создание пользователя
        Integer userId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
        createdUserIds.add(userId);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создаем аккаунт1 (счет)
        Integer firstAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // создаем аккаунт2 (счет)
        Integer secondAccountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунты у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItems(firstAccountId, secondAccountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(firstAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // проверяем наличие депозита
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)));

        // пользователь делает трансфер с firstAccountId на nonExistingAccountId
        Integer nonExistingAccountId = Integer.MAX_VALUE;
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 10
                        }
                        """.formatted(nonExistingAccountId, secondAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        // проверяю, что трансфера не было
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(firstAccountId, secondAccountId))

                // счет отправителя не изменился
                .body("find { it.id == %d }.balance".formatted(firstAccountId),
                        Matchers.equalTo(10.0f))
                .body("find { it.id == %d }.transactions".formatted(firstAccountId),
                        Matchers.hasSize(1))
                .body("find { it.id == %d }.transactions.amount".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(10.0f)))
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo("DEPOSIT")))
                .body("find { it.id == %d }.transactions.relatedAccountId".formatted(firstAccountId),
                        Matchers.everyItem(Matchers.equalTo(firstAccountId)))

                // у отправителя нет операции списания
                .body("find { it.id == %d }.transactions.type".formatted(firstAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_OUT")))

                // счет получателя не изменился
                .body("find { it.id == %d }.balance".formatted(secondAccountId),
                Matchers.equalTo(0.0f))
                .body("find { it.id == %d }.transactions".formatted(secondAccountId),
                        Matchers.hasSize(0))

                // у получателя нет операции зачисления
                .body("find { it.id == %d }.transactions.type".formatted(secondAccountId),
                        Matchers.not(Matchers.hasItem("TRANSFER_IN")));
    }
}
