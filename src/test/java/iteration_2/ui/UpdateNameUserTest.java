package iteration_2.ui;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.CustomerResponse;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import common.annotations.UserSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import storage.SessionStorage;
import ui.pages.BankAlert;
import ui.pages.EditProfilePage;

public class UpdateNameUserTest extends BaseUITest {

    @Disabled("Баг: имя не обновляется без рефреша страницы в .user-name")
    @Test
    @UserSession
    public void userCanUpdateNameWithValidDataTest(){
        UserSteps userSteps = SessionStorage.getSteps();
        String name = RandomData.getValidName();

        new EditProfilePage()
                .open()
                .setName(name)
                .clickSaveChangeButton()
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATE_SUCCESSFULLY.getMessage())
                .checkUserNameLabel(name)
                .openDashboard()
                .checkWelcomeUserName(name);

        CustomerResponse updatedName = userSteps.getCustomerProfile();
        softly.assertThat(updatedName.getName()).isEqualTo(name);
    }

    @Test
    public void userCannotUpdateNameWithInvalidDataTest(){
        String name = RandomData.getNameWithoutSurname();

        new EditProfilePage()
                .open()
                .setName(name)
                .clickSaveChangeButton()
                .checkAlertMessageAndAccept(BankAlert.NAME_MUST_CONTAIN_TWO_WORDS_WITH_LETTERS_ONLY.getMessage());
    }
}
