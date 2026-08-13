package iteration_1.ui;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.comparison.ModelAssertions;
import api.requests.steps.AdminSteps;
import common.annotations.AdminSession;
import org.junit.jupiter.api.Test;
import storage.SessionStorage;
import ui.elements.UserBage;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CreateUserTest extends BaseUITest {

    @Test
    @AdminSession
    public void adminCanCrateUserTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        UserBage newUserBage = new AdminPanel().open().createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USER_CREATED_SUCCESSFULLY.getMessage())
                .findUserByUsername(newUser.getUsername());

        assertThat(newUserBage)
                .as("User should not exist on Dashboard after user creation").isNotNull();

        CreateUserResponse createdUser =
                AdminSteps.getAllUsers().stream().filter(user -> user.getUsername().equals(newUser.getUsername()))
                        .findFirst().get();

        SessionStorage.addUser(newUser, createdUser.getId());

        ModelAssertions.assertThatModels(newUser, createdUser).match();
    }

    @Test
    @AdminSession
    public void adminCannotCrateUserWithInvalidDataTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);
        newUser.setUsername("a");

        assertTrue(new AdminPanel().open().createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage())
                .getAllUsers().stream().noneMatch(userBage -> userBage.getUsername().equals(newUser.getUsername())));

        long usersWithSameUsernameAsNewUser =
                AdminSteps.getAllUsers().stream().filter(user -> user.getUsername().equals(newUser.getUsername())).count();

        assertThat(usersWithSameUsernameAsNewUser).isZero();
    }
}
