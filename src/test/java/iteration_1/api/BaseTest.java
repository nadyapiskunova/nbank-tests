package iteration_1.api;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.List;

public class BaseTest {
    protected List<Integer> createdUserIds = new ArrayList<>();
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
        for (Integer userId : createdUserIds) {
            new CrudRequester(
                    RequestSpecs.adminSpec(),
                    ResponseSpecs.requestReturnsOK(), Endpoint.ADMIN_USER)
                    .delete(userId);
        }
        createdUserIds.clear();
    }
}
