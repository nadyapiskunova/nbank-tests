package iteration_2.api;

import common.extensions.AdminSessionExtension;
import common.extensions.UserSessionExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import org.junit.jupiter.api.extension.ExtendWith;
import storage.SessionStorage;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(AdminSessionExtension.class)
@ExtendWith(UserSessionExtension.class)
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

    protected void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }
}