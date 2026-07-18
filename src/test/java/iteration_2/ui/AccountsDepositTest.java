package iteration_2.ui;

import api.constans.TestConstants;
import api.generators.RandomData;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.DepositPage;

public class AccountsDepositTest extends BaseUITest {
    @Test
    public void userCanDepositWithValidData(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse createdAccount = UserSteps.createAccount(user);
        authAsUser(user);

        Double amount = RandomData.getValidDepositAmount();
        new DepositPage()
                .open()
                .openSelectorAccounts(createdAccount.getAccountNumber())
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_DEPOSITED.getMessage())
                .open()
                .openSelectorAccounts(createdAccount.getAccountNumber())
                .checkAccountBalance(amount, createdAccount.getAccountNumber());

        AccountResponse depositedAccount = UserSteps.getAccounts(user).get(0);
        softly.assertThat(depositedAccount.getBalance()).isEqualTo(amount);
    }

    @Test
    public void userCannotDepositWithInvalidData(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse createdAccount = UserSteps.createAccount(user);
        authAsUser(user);

        double amount = TestConstants.ZERO_AMOUNT;
        new DepositPage()
                .open()
                .openSelectorAccounts(createdAccount.getAccountNumber())
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(BankAlert.PLEASE_ENTER_VALID_AMOUNT.getMessage());
    }

    @Test
    public void userCannotDepositWithoutSelectedAccountTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        UserSteps.createAccount(user);
        authAsUser(user);

        new DepositPage()
                .open()
                .clickDeposit()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_SELECT_ACCOUNT.getMessage());
    }
}




