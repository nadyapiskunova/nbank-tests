package iteration_1.ui;

import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class CreateAccountTest extends BaseUITest {
    @Test
    public void userCanCreateAccountTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        authAsUser(user);

        new UserDashboard().open().createUserAccount();

       List<AccountResponse> createdAccounts =
                UserSteps.getAccounts(user);

        assertThat(createdAccounts).hasSize(1);

        new UserDashboard().checkAlertMessageAndAccept(BankAlert.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance()).isZero();
    }
}
