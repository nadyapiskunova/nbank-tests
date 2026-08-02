package iteration_1.api;

import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.extensions.AdminSessionExtension;
import common.extensions.TimingExtension;
import common.extensions.UserSessionExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import storage.SessionStorage;

@ExtendWith(AdminSessionExtension.class)
@ExtendWith(UserSessionExtension.class)
@ExtendWith(TimingExtension.class)
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
