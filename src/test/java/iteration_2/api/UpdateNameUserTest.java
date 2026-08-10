package iteration_2.api;

import api.constans.ErrorMessages;
import api.contract.BackendVersion;
import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.CustomerResponse;
import api.models.UpdateProfileRequest;
import api.models.comparison.ModelAssertions;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.APIVersion;
import common.annotations.UserSession;
import common.helpers.DbCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import storage.SessionStorage;

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
    @APIVersion(BackendVersion.WITH_VALIDATION_FIX)
    @MethodSource("dataForUserCanUpdateNameWithValidDataTest")
    @ParameterizedTest
    @UserSession
    public void userCanUpdateNameWithValidDataTest(String name) {
        CreateUserRequest user = SessionStorage.getUser();

        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(name)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .update(updateName);

        CustomerResponse updatedName =
                new ValidatedCrudRequester<CustomerResponse>(
                        RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                        Endpoint.CUSTOMER_PROFILE,
                        ResponseSpecs.requestReturnsOK())
                        .get();
        ModelAssertions.assertThatModels(updateName, updatedName).match();
    }

    @Test
    @UserSession
    public void unauthorizedUserCannotUpdateNameTest() {
        CreateUserRequest user = SessionStorage.getUser();

        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(RandomData.getValidName())
                .build();

         new CrudRequester(RequestSpecs.unauthSpec(),
                 Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsUnauthorized())
                .update(updateName);

        CustomerResponse updatedName =
                new ValidatedCrudRequester<CustomerResponse>(
                        RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                        Endpoint.CUSTOMER_PROFILE,
                        ResponseSpecs.requestReturnsOK())
                        .get();

        softly.assertThat(updatedName.getName()).isNull();
        DbCheck.run(() ->
                softly.assertThat(
                        DataBaseSteps.getUserByUsername(user.getUsername()).getName()
                ).isNull()
        );
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
    @UserSession
    public void userCannotUpdateNameWithInValidDataTest(String name, String errorValue) {
        CreateUserRequest user = SessionStorage.getUser();

        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(name)
                .build();

        new CrudRequester(RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .update(updateName);

        CustomerResponse updatedName = new ValidatedCrudRequester<CustomerResponse>(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
        softly.assertThat(updatedName.getName()).isNull();


        DbCheck.run(() ->
                softly.assertThat(
                        DataBaseSteps.getUserByUsername(user.getUsername()).getName()
                ).isNull()
        );
    }
}