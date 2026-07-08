package iteration_1.ui;

import api.models.AccountResponse;
import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import storage.SessionStorage;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class CreateAccountTest extends BaseUITest {

    @Test
    @UserSession
    public void userCanCreateAccountTest(){

        new UserDashboard().open().createUserAccount();

        List<AccountResponse> createdAccounts = SessionStorage.getSteps().getAllAccounts();

        assertThat(createdAccounts).hasSize(1);

        new UserDashboard().checkAlertMessageAndAccept(BankAlert.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance()).isZero();
    }
}
