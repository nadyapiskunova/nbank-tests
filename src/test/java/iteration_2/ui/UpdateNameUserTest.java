package iteration_2.ui;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.CustomerResponse;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.EditProfilePage;

public class UpdateNameUserTest extends BaseUITest {

    @Disabled("Баг: имя не обновляется без рефреша страницы в .user-name")
    @Test
    public void userCanUpdateNameWithValidDataTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        String name = RandomData.getValidName();

        authAsUser(user);
        new EditProfilePage()
                .open()
                .setName(name)
                .clickSaveChangeButton()
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATE_SUCCESSFULLY.getMessage())
                .checkUserNameLabel(name)
                .openDashboard()
                .checkWelcomeUserName(name);

        CustomerResponse updatedName = UserSteps.getCustomerProfile(user);
        softly.assertThat(updatedName.getName()).isEqualTo(name);
    }

    @Test
    public void userCannotUpdateNameWithInvalidDataTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        String name = RandomData.getNameWithoutSurname();

        authAsUser(user);
        new EditProfilePage()
                .open()
                .setName(name)
                .clickSaveChangeButton()
                .checkAlertMessageAndAccept(BankAlert.NAME_MUST_CONTAIN_TWO_WORDS_WITH_LETTERS_ONLY.getMessage());
    }
}
