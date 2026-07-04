package iteration_2.ui;

import api.constans.TestConstants;
import api.generators.RandomData;
import api.models.*;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.TransferPage;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.within;

public class AccountsTransferTest extends BaseUITest {
    @Test
    public void userCanTransferWithValidDataTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
                UserSteps.deposit(
                        user,
                        firstAccount.getId(),
                        TestConstants.MAX_DEPOSIT_AMOUNT
                );

        authAsUser(user);
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

        List<AccountResponse> accountsAfterTransfer = UserSteps.getAccounts(user);

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
    public void userCannotTransferWithInvalidDataTest() {
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        authAsUser(user);
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
    public void userCannotTransferWithoutConfirmTest() {
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        authAsUser(user);
        double amount = RandomData.getTransferAmount();;
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
    public void userCanSearchTransactionWithValidName(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        UpdateProfileRequest updatedName = UserSteps.updateName(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        authAsUser(user);
        new TransferPage()
                .open()
                .clickTransferAgainButton()
                .searchByName(updatedName.getName())
                .clickSearchTransactionButton()
                .checkFoundUnder(updatedName.getName());
    }

    @Test
    public void userCannotSearchTransactionWithInvalidName(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        UserSteps.updateName(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );

        authAsUser(user);
        new TransferPage()
                .open()
                .clickTransferAgainButton()
                .searchByName(RandomData.getNameWithoutSurname())
                .clickSearchTransactionButton()
                .checkAlertMessageAndAccept(BankAlert.NO_MATCHING_USERS_FOUND.getMessage());
    }

    @Disabled("Баг: в popup повтора операции TRANSFER_IN отображается firstAccount.getId()")
    @Test
    public void userCanRepeatTransfer(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse firstAccount = UserSteps.createAccount(user);
        AccountResponse secondAccount = UserSteps.createAccount(user);
        UserSteps.deposit(
                user,
                firstAccount.getId(),
                TestConstants.MAX_DEPOSIT_AMOUNT
        );
        double amountTransfer = RandomData.getTransferAmount();
        UserSteps.transfer(
                user,
                firstAccount.getId(),
                secondAccount.getId(),
                amountTransfer);

        authAsUser(user);
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

        List<AccountResponse> accountsAfterRepeatTransfer = UserSteps.getAccounts(user);

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
