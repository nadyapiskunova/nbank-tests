package iteration_1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class CreateUserTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter())
        );
    }

    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                        {
                          "username": "kate040",
                          "password": "Kat#e2000",
                          "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", Matchers.equalTo("kate040"))
                .body("password", Matchers.not(Matchers.equalTo("Kat#e2000")))
                .body("role", Matchers.equalTo("USER"));

    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                // username field validation
                Arguments.of(
                        " ",
                        "Password33$",
                        "USER",
                        List.of("Username cannot be blank",
                                "Username must be between 3 and 15 characters",
                                "Username must contain only letters, digits, dashes, underscores, and dots")),

                Arguments.of(
                        "ab",
                        "Password33$",
                        "USER",
                        List.of("Username must be between 3 and 15 characters")),

                Arguments.of(
                        "abc$",
                        "Password33$",
                        "USER",
                        List.of("Username must contain only letters, digits, dashes, underscores, and dots")),

                Arguments.of(
                        "abc%",
                        "Password33$",
                        "USER",
                        List.of("Username must contain only letters, digits, dashes, underscores, and dots"))
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidDataTest(String username, String password, String role, List<String> errors) {
        String requestBody = String.format(
                """
                        {
                        "username": "%s",
                                 "password": "%s",
                                 "role": "%s"
                        }
                        """, username, password, role);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("username", Matchers.containsInAnyOrder(errors.toArray()));
    }


}
