package iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import requests.AdminDeleteUserRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.List;

public class BaseTest {
    protected List<Integer> createdUserIds = new ArrayList<>();
    protected String username;
    protected String secondUsername;
    protected String password;
    protected SoftAssertions softly;

    @BeforeAll
    public static void setUpRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter())
        );
    }

    @BeforeEach
    public void setupTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterTest() {
        softly.assertAll();
    }

    @AfterEach
    public void deleteUsers() {
        for (Integer userId : createdUserIds) {
            new AdminDeleteUserRequester(
                    RequestSpecs.adminSpec(),
                    ResponseSpecs.requestReturnsOK())
                    .delete(userId);
        }
        createdUserIds.clear();
    }

    protected void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }
}
