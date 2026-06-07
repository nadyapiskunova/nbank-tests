package iteration_2;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

public class AccountsDepositTest extends BaseTest {

    public static Stream<Arguments> validDataForUserCanDepositWithValidDataTest(){

        return Stream.of(
                Arguments.of(0.01),
                Arguments.of(4999.99),
                Arguments.of(5000.00)
        );
    }
    @MethodSource("validDataForUserCanDepositWithValidDataTest")
    @ParameterizedTest
    public void userCanDepositWithValidDataTest(Double amount) {
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
        Integer accountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": %s
                        }
                        """.formatted(accountId, amount))
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
                .body("[0].id", Matchers.equalTo(accountId))
                .body("[0].balance", Matchers.equalTo(amount.floatValue()))
                .body("[0].transactions", Matchers.hasSize(1))
                .body("[0].transactions[0].amount", Matchers.equalTo(amount.floatValue()))
                .body("[0].transactions[0].type", Matchers.equalTo("DEPOSIT"));
    }

    public static Stream<Arguments> dataForUserCannotDepositWithInvalidAmountTest(){

        return Stream.of(
                Arguments.of(-1, "Deposit amount must be at least 0.01"),
                Arguments.of(0, "Deposit amount must be at least 0.01"),
                Arguments.of(5000.01, "Deposit amount cannot exceed 5000")
        );
    }
    @MethodSource("dataForUserCannotDepositWithInvalidAmountTest")
    @ParameterizedTest
    public void userCannotDepositWithInvalidAmountTest(Double amount, String errorValue) {
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
        Integer accountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountId));

        // пользователь делает депозит на аккаунт
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": %s
                        }
                        """.formatted(accountId, amount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));

        // проверяем, что депозита нет
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].id", Matchers.equalTo(accountId))
                .body("[0].balance", Matchers.equalTo(0.0F))
                .body("[0].transactions", Matchers.empty());
    }

    @Test
    public void adminCannotDepositToUserAccountTest() {
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
        Integer accountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountId));

        // админ делает депозит на аккаунт
        given()
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(accountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // проверяем, что депозит не добавлен
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].id", Matchers.equalTo(accountId))
                .body("[0].balance", Matchers.equalTo(0.0F))
                .body("[0].transactions", Matchers.empty());
    }

    @Test
    public void authorizedUserCannotDepositToNonExistentAccountTest(){
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
        Integer accountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountId));

        // пользователь делает депозит на не существующий аккаунт
        Integer nonExistingAccountId = Integer.MAX_VALUE;

        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(nonExistingAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        // проверяем, что депозит не добавлен
        given()
                .header("Authorization", userAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].id", Matchers.equalTo(accountId))
                .body("[0].balance", Matchers.equalTo(0.0F))
                .body("[0].transactions", Matchers.empty());
    }

    @Test
    public void authorizedUserCannotDepositToAnotherUsersAccountTest(){
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


        // пользователь 1 делает депозит на accountIdBySecondUser
        given()
                .header("Authorization", firstUserAuthHeader)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(accountIdBySecondUser))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        // проверяем, что депозит не добавлен
        given()
                .header("Authorization", secondUserAuthHeader)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].id", Matchers.equalTo(accountIdBySecondUser))
                .body("[0].balance", Matchers.equalTo(0.0F))
                .body("[0].transactions", Matchers.empty());
    }

    @Test
    public void unauthorizedUserCannotDepositFundsToAccountTest(){
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
        Integer accountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // проверяем что аккаунт у юзера есть
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", hasItem(accountId));

        // пользователь делает депозит на аккаунт
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "id": %d,
                        "balance": 10
                        }
                        """.formatted(accountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
