package iteration_1.api;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import storage.SessionStorage;

import java.util.ArrayList;
import java.util.List;

public class BaseTest {
    protected SoftAssertions softly;

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
        for (Integer userId : SessionStorage.getCreatedUserIds()) {
            new CrudRequester(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_USER,
                    ResponseSpecs.requestReturnsOK())
                    .delete(userId);
        }
        SessionStorage.clear();
    }
}
