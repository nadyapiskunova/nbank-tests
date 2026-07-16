package iteration_1.api;

import api.constans.ErrorMessages;
import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import storage.SessionStorage;

import java.util.List;
import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {
    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        CreateUserRequest createUserRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse createUserResponse = new ValidatedCrudRequester<CreateUserResponse>
                (RequestSpecs.adminSpec(),
                        Endpoint.ADMIN_USER,
                        ResponseSpecs.entityWasCreated())
                .post(createUserRequest);
        SessionStorage.addUser(createUserRequest, createUserResponse.getId());

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
                 Endpoint.ADMIN_USER,
                ResponseSpecs.requestReturnsBadRequest("username", errors))
                .post(createUserRequest);
    }
}