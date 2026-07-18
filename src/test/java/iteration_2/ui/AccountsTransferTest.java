package iteration_2.ui;

import api.constans.TestConstants;
import api.generators.RandomData;
import api.models.*;
import api.requests.steps.UserSteps;
import common.annotations.UserSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import storage.SessionStorage;
import ui.pages.BankAlert;
import ui.pages.TransferPage;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.within;

public class AccountsTransferTest extends BaseUITest {
    @Test
    @UserSession
    public void userCanTransferWithValidDataTest(){
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        AccountResponse secondAccount = userSteps.createAccount();
        userSteps.deposit(
                        firstAccount.getId(),
                        TestConstants.MAX_DEPOSIT_AMOUNT
                );

        double amount = RandomData.getTransferAmount();
        new TransferPage()
                .open()
                .selectAccount(firstAccount.getAccountNumber())
                .setUsername(user.getUsername())
                .setReceiverAccountNumber(secondAccount.getAccountNumber())
                .setAmount(amount)
                .confirmCheckbox()
                .clickTransferButton()
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_TRANSFERRED.getMessage())
                .open()
                .clickTransferAgainButton()
                .checkTransactionIsDisplayed(TransactionType.TRANSFER_IN, amount)
                .checkTransactionIsDisplayed(TransactionType.TRANSFER_OUT, amount);

        List<AccountResponse> accountsAfterTransfer = userSteps.getAllAccounts();

        AccountResponse senderAccount = accountsAfterTransfer.stream()
                .filter(account -> account.getId().equals(firstAccount.getId()))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterTransfer.stream()
                .filter(account -> account.getId().equals(secondAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isCloseTo(TestConstants.MAX_DEPOSIT_AMOUNT - amount, within(0.001));

        softly.assertThat(receiverAccount.getBalance())
                .isCloseTo(amount, within(0.001));

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_OUT);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_IN);

    }

    @Test
    @UserSession
    public void userCannotTransferWithInvalidDataTest() {
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        AccountResponse secondAccount = userSteps.createAccount();

        userSteps.deposit(
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        double amount = TestConstants.ZERO_AMOUNT;
        new TransferPage()
                .open()
                .selectAccount(firstAccount.getAccountNumber())
                .setUsername(user.getUsername())
                .setReceiverAccountNumber(secondAccount.getAccountNumber())
                .setAmount(amount)
                .confirmCheckbox()
                .clickTransferButton()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_AMOUNT_MUST_BE_AT_LEAST_0_01.getMessage())
                .open()
                .clickTransferAgainButton()
                .checkTransferTransactionsAreNotDisplayed();
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutConfirmTest() {
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        AccountResponse secondAccount = userSteps.createAccount();
        userSteps.deposit(
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        double amount = RandomData.getTransferAmount();
        new TransferPage()
                .open()
                .selectAccount(firstAccount.getAccountNumber())
                .setUsername(user.getUsername())
                .setReceiverAccountNumber(secondAccount.getAccountNumber())
                .setAmount(amount)
                .clickTransferButton()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage())
                .open()
                .clickTransferAgainButton()
                .checkTransferTransactionsAreNotDisplayed();
    }

    @Test
    @UserSession
    public void userCanSearchTransactionWithValidName(){
        UserSteps userSteps = SessionStorage.getSteps();
        AccountResponse firstAccount = userSteps.createAccount();
        UpdateProfileRequest updatedName = userSteps.updateName();
        userSteps.deposit(
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        new TransferPage()
                .open()
                .clickTransferAgainButton()
                .searchByName(updatedName.getName())
                .clickSearchTransactionButton()
                .checkFoundUnder(updatedName.getName());
    }

    @Test
    @UserSession
    public void userCannotSearchTransactionWithInvalidName(){
        UserSteps userSteps = SessionStorage.getSteps();
        AccountResponse firstAccount = userSteps.createAccount();
        userSteps.updateName();
        userSteps.deposit(
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        new TransferPage()
                .open()
                .clickTransferAgainButton()
                .searchByName(RandomData.getNameWithoutSurname())
                .clickSearchTransactionButton()
                .checkAlertMessageAndAccept(BankAlert.NO_MATCHING_USERS_FOUND.getMessage());
    }

    @Disabled("Баг: в popup повтора операции TRANSFER_IN отображается firstAccount.getId()")
    @Test
    @UserSession
    public void userCanRepeatTransfer(){
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        AccountResponse secondAccount = userSteps.createAccount();
        userSteps.deposit(
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        double amountTransfer = RandomData.getTransferAmount();
        userSteps.transfer(
                firstAccount.getId(),
                secondAccount.getId(),
                amountTransfer);

        new TransferPage()
                .open()
                .clickTransferAgainButton()
                .repeatTransaction(TransactionType.TRANSFER_IN)
                .checkAccountId(secondAccount.getId())
                .selectSenderAccountNumber(firstAccount.getAccountNumber())
                .confirmCheckbox()
                .clickSendTransferButton()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_OF_SUCCESSFULLY.getMessage());

        double expectedSenderBalance =
                TestConstants.MAX_DEPOSIT_AMOUNT - amountTransfer - amountTransfer;

        double expectedReceiverBalance = amountTransfer + amountTransfer;

        List<AccountResponse> accountsAfterRepeatTransfer = userSteps.getAllAccounts();

        AccountResponse senderAccount = accountsAfterRepeatTransfer.stream()
                .filter(account -> account.getId().equals(firstAccount.getId()))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterRepeatTransfer.stream()
                .filter(account -> account.getId().equals(secondAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance()).isCloseTo(expectedSenderBalance, within(0.001));
        softly.assertThat(receiverAccount.getBalance()).isCloseTo(expectedReceiverBalance, within(0.001));

    }
}
