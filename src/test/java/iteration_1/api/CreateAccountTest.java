package iteration_1.api;

import api.models.CreateUserRequest;
import org.junit.jupiter.api.Test;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

public class CreateAccountTest extends BaseTest {
    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated(), Endpoint.ACCOUNTS)
                .post();
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated(), Endpoint.ACCOUNTS)
                .post();
    }
}