package requests.steps;

import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

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