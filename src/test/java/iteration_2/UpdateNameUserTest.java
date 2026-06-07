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

public class UpdateNameUserTest extends BaseTest {

    public static Stream<Arguments> dataForUserCanUpdateNameWithValidDataTest() {
        return Stream.of(
                Arguments.of("Kate Smith"),
                // нет требований по ограничению max/min символов

                // max: 50 символов для имени и фамилии
                Arguments.of("KateKateKateKateKateKateKateKateKateKateKateKateKa "
                        + "SmithSmithSmithSmithSmithSmithSmithSmithSmithSmith"),
                // min: 1 символ для имени и фамилии
                Arguments.of("K S")
        );
    }
    @MethodSource("dataForUserCanUpdateNameWithValidDataTest")
    @ParameterizedTest
    public void userCanUpdateNameWithValidDataTest(String name) {
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

        // меняю имя
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("message", Matchers.equalTo("Profile updated successfully"))
                .body("customer.name", Matchers.equalTo(name));

        // проверяю что имя сохранилось
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(name));
    }

    @Test
    public void authenticatedUserCannotUpdateNameTest() {
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

        // меняю имя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "name": "Kate Smith"
                        }
                        """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        // проверяю что имя не сохранилось
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(null));
    }

    public static Stream<Arguments> dataForUserCannotUpdateNameWithInvalidDataTest() {
        return Stream.of(
                Arguments.of("", "Name must contain two words with letters only"),
                Arguments.of("   ", "Name must contain two words with letters only"),
                Arguments.of("Kate", "Name must contain two words with letters only"),
                Arguments.of("Kate1 Smith", "Name must contain two words with letters only"),
                Arguments.of("Kate Smith#", "Name must contain two words with letters only")
        );
    }
    @MethodSource("dataForUserCannotUpdateNameWithInvalidDataTest")
    @ParameterizedTest
    public void userCannotUpdateNameWithValidDataTest(String name, String errorValue) {
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

        // меняю имя
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));

        // проверяю что имя не сохранилось
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(null));
    }
}
