package iteration_2.ui;

import api.constans.TestConstants;
import api.generators.RandomData;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import storage.SessionStorage;
import ui.pages.BankAlert;
import ui.pages.DepositPage;

public class AccountsDepositTest extends BaseUITest {
    @Test
    @UserSession
    public void userCanDepositWithValidData(){
        UserSteps userSteps = SessionStorage.getSteps();
        AccountResponse createdAccount = userSteps.createAccount();

        Double amount = RandomData.getValidDepositAmount();
        new DepositPage()
                .open()
                .openSelectorAccounts(createdAccount.getAccountNumber())
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_DEPOSITED.getMessage())
                .open()
                .openSelectorAccounts(createdAccount.getAccountNumber())
                .checkAccountBalance(amount, createdAccount.getAccountNumber());

        AccountResponse depositedAccount = userSteps.getAllAccounts().get(0);
        softly.assertThat(depositedAccount.getBalance()).isEqualTo(amount);
    }

    @Test
    @UserSession
    public void userCannotDepositWithInvalidData(){
        UserSteps userSteps = SessionStorage.getSteps();
        AccountResponse createdAccount = userSteps.createAccount();

        double amount = TestConstants.ZERO_AMOUNT;
        new DepositPage()
                .open()
                .openSelectorAccounts(createdAccount.getAccountNumber())
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(BankAlert.PLEASE_ENTER_VALID_AMOUNT.getMessage());
    }

    @Test
    @UserSession
    public void userCannotDepositWithoutSelectedAccountTest(){
        UserSteps userSteps = SessionStorage.getSteps();

        userSteps.createAccount();

        new DepositPage()
                .open()
                .clickDeposit()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_SELECT_ACCOUNT.getMessage());
    }
}




