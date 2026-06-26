package iteration_1;

import constans.ErrorMessages;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {
    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        CreateUserRequest createUserRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse createUserResponse = new ValidatedCrudRequester<CreateUserResponse>
                (RequestSpecs.adminSpec(),
                        ResponseSpecs.entityWasCreated(),
                        Endpoint.ADMIN_USER)
                .post(createUserRequest);

        ModelAssertions.assertThatModels(createUserRequest, createUserResponse).match();
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                // username field validation
                Arguments.of(
                        " ",
                        "Password33$",
                        "USER",
                        List.of(ErrorMessages.USERNAME_BLANK,
                                ErrorMessages.USERNAME_LENGTH,
                                ErrorMessages.USERNAME_INVALID_CHARS)),

                Arguments.of(
                        "ab",
                        "Password33$",
                        "USER",
                        List.of(ErrorMessages.USERNAME_LENGTH)),

                Arguments.of(
                        "abc$",
                        "Password33$",
                        "USER",
                        List.of(ErrorMessages.USERNAME_INVALID_CHARS)),

                Arguments.of(
                        "abc%",
                        "Password33$",
                        "USER",
                        List.of(ErrorMessages.USERNAME_INVALID_CHARS))
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidDataTest(String username, String password, String role, List<String> errors) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

         new CrudRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest("username", errors),
                 Endpoint.ADMIN_USER)
                .post(createUserRequest);
    }
}