package api.requests.steps;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class AdminSteps {

    public static CreateUserRequest createUser(List<Integer> createdUserIds) {
        CreateUserRequest userRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse userResponse =
                new ValidatedCrudRequester<CreateUserResponse>(
                        RequestSpecs.adminSpec(),
                        ResponseSpecs.entityWasCreated(),
                        Endpoint.ADMIN_USER)
                        .post(userRequest);

        createdUserIds.add(userResponse.getId());

        return userRequest;
    }
}