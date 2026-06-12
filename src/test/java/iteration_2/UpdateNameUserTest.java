package iteration_2;

import constans.ErrorMessages;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.GetProfileRequester;
import requests.UpdateProfileRequester;
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
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // меняю имя
        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(name)
                .build();

        new UpdateProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .put(updateName)
                .extract()
                .as(UpdateProfileResponse.class);

        // проверяю, что имя изменилось
        CustomerResponse updatedName = new GetProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(CustomerResponse.class);

        softly.assertThat(updatedName.getName()).isEqualTo(name);
    }

    @Test
    public void unauthorizedUserCannotUpdateNameTest() {
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // меняю имя
        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(RandomData.getValidName())
                .build();

         new UpdateProfileRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .put(updateName);

        CustomerResponse updatedName = new GetProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(CustomerResponse.class);

        softly.assertThat(updatedName.getName()).isEqualTo(userResponse.getName());
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
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // меняю имя
        UpdateProfileRequest updateName = UpdateProfileRequest.builder()
                .name(name)
                .build();

        new UpdateProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .put(updateName);

        // проверяю, что имя не изменилось
        CustomerResponse updatedName = new GetProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(CustomerResponse.class);
        softly.assertThat(updatedName.getName()).isEqualTo(userResponse.getName());
    }
}