package iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

public class BaseTest {
    protected List<Integer> createdUserIds = new ArrayList<>();
    protected String username;
    protected String secondUsername;
    protected String password;

    @BeforeAll
    public static void setUpRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter())
        );
    }

    @BeforeEach
    public void setUpData() {
        username = "kate" + (System.currentTimeMillis() % 100000);
        secondUsername = "user2" + (System.currentTimeMillis() % 100000);
        password = "Kat#e2000";
    }

    @AfterEach
    public void deleteUsers() {
        for (Integer userId : createdUserIds) {
            given()
                    .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                    .delete("http://localhost:4111/api/v1/admin/users/" + userId)
                    .then()
                    .statusCode(HttpStatus.SC_OK);

        }
        createdUserIds.clear();
    }
}
