package iteration_2;

import constans.ErrorMessages;
import generators.RandomData;
import models.*;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class UpdateNameUserTest extends BaseTest {

    public static Stream<Arguments> dataForUserCanUpdateNameWithValidDataTest() {
        return Stream.of(
                // нет требований по ограничению max/min символов
                // max: 50 символов для имени и фамилии
                Arguments.of(RandomData.getMaxLengthName()),
                // min: 1 символ для имени и фамилии
                Arguments.of(RandomData.getMinLengthName())
        );
    }
    @MethodSource("dataForUserCanUpdateNameWithValidDataTest")
    @ParameterizedTest
    public void userCanUpdateNameWithValidDataTest(String name) {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(name)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                Endpoint.UPDATE_CUSTOMER_PROFILE)
                .update(updateName);

        CustomerResponse updatedName =
                new ValidatedCrudRequester<CustomerResponse>(
                        RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK(),Endpoint.CUSTOMER_PROFILE)
                        .get();

        ModelAssertions.assertThatModels(updateName, updatedName).match();
    }

    @Test
    public void unauthorizedUserCannotUpdateNameTest() {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(RandomData.getValidName())
                .build();

         new CrudRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized(),Endpoint.UPDATE_CUSTOMER_PROFILE)
                .update(updateName);

        CustomerResponse updatedName =
                new ValidatedCrudRequester<CustomerResponse>(
                        RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK(), Endpoint.CUSTOMER_PROFILE)
                        .get();

        softly.assertThat(updatedName.getName()).isNull();
    }

    public static Stream<Arguments> dataForUserCannotUpdateNameWithInvalidDataTest() {
        return Stream.of(
                Arguments.of("", ErrorMessages.INVALID_NAME),
                Arguments.of("   ", ErrorMessages.INVALID_NAME),
                Arguments.of(RandomData.getNameWithoutSurname(), ErrorMessages.INVALID_NAME),
                Arguments.of(RandomData.getNameWithDigits(), ErrorMessages.INVALID_NAME),
                Arguments.of(RandomData.getNameWithSpecialCharacter(), ErrorMessages.INVALID_NAME)
        );
    }
    @MethodSource("dataForUserCannotUpdateNameWithInvalidDataTest")
    @ParameterizedTest
    public void userCannotUpdateNameWithInValidDataTest(String name, String errorValue) {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(name)
                .build();

        new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue),
                Endpoint.UPDATE_CUSTOMER_PROFILE)
                .update(updateName);

        CustomerResponse updatedName = new ValidatedCrudRequester<CustomerResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                Endpoint.CUSTOMER_PROFILE)
                .get();
        softly.assertThat(updatedName.getName()).isNull();
    }
}