package iteration_2;

import constans.ErrorMessages;
import constans.Messages;
import constans.TestConstants;
import generators.RandomData;
import models.*;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.within;

public class AccountsTransferTest extends BaseTest {
    public static Stream<Arguments> validTransferAmounts() {

        return Stream.of(
                Arguments.of(TestConstants.MIN_AMOUNT),
                Arguments.of(TestConstants.ALMOST_MAX_TRANSFER_AMOUNT),
                Arguments.of(TestConstants.MAX_TRANSFER_AMOUNT)
        );
    }

    public static Stream<Arguments> invalidTransferAmounts() {

        return Stream.of(
                Arguments.of(TestConstants.NEGATIVE_AMOUNT, ErrorMessages.TRANSFER_AMOUNT_MIN),
                Arguments.of(TestConstants.ZERO_AMOUNT, ErrorMessages.TRANSFER_AMOUNT_MIN),
                Arguments.of(TestConstants.ABOVE_MAX_TRANSFER_AMOUNT, ErrorMessages.TRANSFER_AMOUNT_MAX)
        );
    }

    @MethodSource("validTransferAmounts")
    @ParameterizedTest
    public void userCanTransferBetweenTheirAccountWithValidDataTest(Double amount) {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse firstAccount = UserSteps.createAccount(userRequest);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = UserSteps.createAccount(userRequest);
        Integer secondAccountId = secondAccount.getId();

        repeat(2, () -> UserSteps.deposit(
                userRequest,
                firstAccountId,
                TestConstants.MAX_DEPOSIT_AMOUNT
        ));

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(amount)
                .build();
        TransferResponse transferToSecondAccountId = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(), Endpoint.TRANSFER)
                .post(transferToSecondAccountIdRequest);

        softly.assertThat(transferToSecondAccountId.getMessage()).isEqualTo(Messages.TRANSFER_SUCCESSFUL);
        ModelAssertions.assertThatModels(
                        transferToSecondAccountIdRequest,
                        transferToSecondAccountId)
                .match();

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 2;
        BigDecimal expectedSenderBalance = BigDecimal.valueOf(expectedBalance)
                .subtract(BigDecimal.valueOf(amount));
        BigDecimal expectedReceiverBalance = BigDecimal.valueOf(amount);

        List<AccountResponse> accountsAfterTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse senderAccount = accountsAfterTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterTransfer.stream()
                .filter(account -> account.getId().equals(secondAccountId))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isCloseTo(expectedSenderBalance.doubleValue(), within(0.001));

        softly.assertThat(receiverAccount.getBalance())
                .isCloseTo(expectedReceiverBalance.doubleValue(), within(0.001));

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_OUT);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_IN);
    }

    @MethodSource("validTransferAmounts")
    @ParameterizedTest
    public void userCanTransferToExternalAccountWithValidDataTest(Double amount) {
        CreateUserRequest firstUserRequest = AdminSteps.createUser(createdUserIds);

        CreateUserRequest secondUserRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse createdAccountForFirstUser = UserSteps.createAccount(firstUserRequest);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = UserSteps.createAccount(secondUserRequest);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        repeat(2, () -> UserSteps.deposit(
                firstUserRequest,
                accountIdByFirstUser,
                TestConstants.MAX_DEPOSIT_AMOUNT
        ));

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(amount)
                .build();
        TransferResponse transferToSecondAccountId = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(), Endpoint.TRANSFER)
                .post(transferToSecondUser);

        softly.assertThat(transferToSecondAccountId.getMessage()).isEqualTo(Messages.TRANSFER_SUCCESSFUL);

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 2;
        BigDecimal expectedSenderBalance = BigDecimal.valueOf(expectedBalance).subtract(BigDecimal.valueOf(amount));
        BigDecimal expectedReceiverBalance = BigDecimal.valueOf(amount);

        List<AccountResponse> accountsByFirstUserAfterTransfer = UserSteps.getAccounts(firstUserRequest);

        AccountResponse firstUserAccount = accountsByFirstUserAfterTransfer.stream()
                .filter(account -> account.getId().equals(accountIdByFirstUser))
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserAccount.getBalance())
                .isCloseTo(expectedSenderBalance.doubleValue(), within(0.001));

        softly.assertThat(firstUserAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_OUT);

        List<AccountResponse> accountsBySecondUserAfterTransfer = UserSteps.getAccounts(secondUserRequest);
        AccountResponse secondUserAccount = accountsBySecondUserAfterTransfer.stream()
                .filter(account -> account.getId().equals(accountIdBySecondUser))
                .findFirst()
                .orElseThrow();

        softly.assertThat(secondUserAccount.getBalance())
                .isCloseTo(expectedReceiverBalance.doubleValue(), within(0.001));

        softly.assertThat(secondUserAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_IN);
    }

    @MethodSource("invalidTransferAmounts")
    @ParameterizedTest
    public void userCannotTransferBetweenTheirAccountWithInvalidDataTest(Double amount, String errorValue) {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse firstAccount = UserSteps.createAccount(userRequest);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = UserSteps.createAccount(userRequest);
        Integer secondAccountId = secondAccount.getId();

        repeat(3, () -> UserSteps.deposit(userRequest, firstAccountId, TestConstants.MAX_DEPOSIT_AMOUNT));

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(amount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue), Endpoint.TRANSFER)
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse senderAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(secondAccountId))
                .findFirst()
                .orElseThrow();

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 3;

        softly.assertThat(senderAccount.getBalance())
                .isEqualTo(expectedBalance);
        softly.assertThat(receiverAccount.getBalance())
                .isEqualTo(0.0);
        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);
        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @MethodSource("invalidTransferAmounts")
    @ParameterizedTest
    public void userCannotTransferToExternalAccountWithInvalidDataTest(Double amount, String errorValue) {
        CreateUserRequest firstUserRequest = AdminSteps.createUser(createdUserIds);

        CreateUserRequest secondUserRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse createdAccountForFirstUser = UserSteps.createAccount(firstUserRequest);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = UserSteps.createAccount(secondUserRequest);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        repeat(3, () -> UserSteps.deposit(
                firstUserRequest,
                accountIdByFirstUser,
                TestConstants.MAX_DEPOSIT_AMOUNT));

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(amount)
                .build();
         new CrudRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue), Endpoint.TRANSFER)
                .post(transferToSecondUser);

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 3;

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer = UserSteps.getAccounts(firstUserRequest);

        AccountResponse firstUserAccountAfterFailedTransfer =
                accountsByFirstUserAfterFailedTransfer.stream()
                        .filter(account -> account.getId().equals(accountIdByFirstUser))
                        .findFirst()
                        .orElseThrow();

        softly.assertThat(firstUserAccountAfterFailedTransfer.getBalance())
                .isEqualTo(expectedBalance);

        softly.assertThat(firstUserAccountAfterFailedTransfer.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);

        List<AccountResponse> accountsBySecondUserAfterFailedTransfer = UserSteps.getAccounts(secondUserRequest);
        AccountResponse secondUserAccountAfterFailedTransfer =
                accountsBySecondUserAfterFailedTransfer.stream()
                        .filter(account -> account.getId().equals(accountIdBySecondUser))
                        .findFirst()
                        .orElseThrow();

        softly.assertThat(secondUserAccountAfterFailedTransfer.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(secondUserAccountAfterFailedTransfer.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @Test
    public void userCannotTransferAmountExceedingBalanceBetweenTheirAccountTest(){
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse firstAccount = UserSteps.createAccount(userRequest);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = UserSteps.createAccount(userRequest);
        Integer secondAccountId = secondAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        UserSteps.deposit(userRequest, firstAccountId, depositAmount);

        double transferAmount = RandomData.getAmountGreaterThan(depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER), Endpoint.TRANSFER)
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse senderAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(secondAccountId))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isEqualTo(depositAmount);

        softly.assertThat(receiverAccount.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @Test
    public void userCannotTransferAmountExceedingBalanceToExternalAccountTest(){
        CreateUserRequest firstUserRequest = AdminSteps.createUser(createdUserIds);

        CreateUserRequest secondUserRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse createdAccountForFirstUser = UserSteps.createAccount(firstUserRequest);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = UserSteps.createAccount(secondUserRequest);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        UserSteps.deposit(firstUserRequest, accountIdByFirstUser, depositAmount);

        double transferAmount = RandomData.getAmountGreaterThan(depositAmount);

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(transferAmount)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER), Endpoint.TRANSFER)
                .post(transferToSecondUser);

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer =UserSteps.getAccounts(firstUserRequest);

        AccountResponse firstUserAccountAfterFailedTransfer =
                accountsByFirstUserAfterFailedTransfer.stream()
                        .filter(account -> account.getId().equals(accountIdByFirstUser))
                        .findFirst()
                        .orElseThrow();

        softly.assertThat(firstUserAccountAfterFailedTransfer.getBalance())
                .isEqualTo(depositAmount);

        softly.assertThat(firstUserAccountAfterFailedTransfer.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);

        List<AccountResponse> accountsBySecondUserAfterFailedTransfer = UserSteps.getAccounts(secondUserRequest);

        AccountResponse secondUserAccountAfterFailedTransfer =
                accountsBySecondUserAfterFailedTransfer.stream()
                        .filter(account -> account.getId().equals(accountIdBySecondUser))
                        .findFirst()
                        .orElseThrow();

        softly.assertThat(secondUserAccountAfterFailedTransfer.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(secondUserAccountAfterFailedTransfer.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @Test
    public void adminCannotTransferBetweenUserAccountsTest(){
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse firstAccount = UserSteps.createAccount(userRequest);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = UserSteps.createAccount(userRequest);
        Integer secondAccountId = secondAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        UserSteps.deposit(userRequest, firstAccountId, depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsForbidden(), Endpoint.TRANSFER)
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse senderAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(secondAccountId))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isEqualTo(depositAmount);

        softly.assertThat(receiverAccount.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @Test
    public void adminCannotTransferFromUserAccountToAnotherUsersAccountTest(){
        CreateUserRequest firstUserRequest = AdminSteps.createUser(createdUserIds);

        CreateUserRequest secondUserRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse createdAccountForFirstUser = UserSteps.createAccount(firstUserRequest);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = UserSteps.createAccount(secondUserRequest);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        double depositAmount = RandomData.getSmallDepositAmount();

        UserSteps.deposit(firstUserRequest, accountIdByFirstUser, depositAmount);

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsForbidden(), Endpoint.TRANSFER)
                .post(transferToSecondUser);

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer = UserSteps.getAccounts(firstUserRequest);

        AccountResponse firstUserAccountAfterFailedTransfer =
                accountsByFirstUserAfterFailedTransfer.stream()
                        .filter(account -> account.getId().equals(accountIdByFirstUser))
                        .findFirst()
                        .orElseThrow();

        softly.assertThat(firstUserAccountAfterFailedTransfer.getBalance())
                .isEqualTo(depositAmount);

        softly.assertThat(firstUserAccountAfterFailedTransfer.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);

        List<AccountResponse> accountsBySecondUserAfterFailedTransfer = UserSteps.getAccounts(secondUserRequest);

        AccountResponse secondUserAccountAfterFailedTransfer =
                accountsBySecondUserAfterFailedTransfer.stream()
                        .filter(account -> account.getId().equals(accountIdBySecondUser))
                        .findFirst()
                        .orElseThrow();

        softly.assertThat(secondUserAccountAfterFailedTransfer.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(secondUserAccountAfterFailedTransfer.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @Test
    public void unauthorizedUserCannotTransferBetweenOwnAccountsTest(){
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse firstAccount = UserSteps.createAccount(userRequest);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = UserSteps.createAccount(userRequest);
        Integer secondAccountId = secondAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        UserSteps.deposit(userRequest, firstAccountId, depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized(), Endpoint.TRANSFER)
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse senderAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(secondAccountId))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isEqualTo(depositAmount);

        softly.assertThat(receiverAccount.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }

    @Test
    public void userCannotTransferToNonExistentAccountTest(){
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse firstAccount = UserSteps.createAccount(userRequest);
        Integer firstAccountId = firstAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        UserSteps.deposit(userRequest, firstAccountId, depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER), Endpoint.TRANSFER)
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse senderAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        softly.assertThat(senderAccount.getBalance())
                .isEqualTo(depositAmount);

        softly.assertThat(senderAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT);
    }

    @Test
    public void userCannotTransferFromNonExistentAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse account = UserSteps.createAccount(userRequest);
        Integer accountId = account.getId();

        double transferAmount = RandomData.getValidDepositAmount();

        TransferRequest transferToAccountRequest = TransferRequest.builder()
                .senderAccountId(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .receiverAccountId(accountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(), Endpoint.TRANSFER)
                .post(transferToAccountRequest);

        List<AccountResponse> accountsAfterFailedTransfer = UserSteps.getAccounts(userRequest);

        AccountResponse receiverAccount = accountsAfterFailedTransfer.stream()
                .filter(accountResponse -> accountResponse.getId().equals(accountId))
                .findFirst()
                .orElseThrow();

        softly.assertThat(receiverAccount.getBalance())
                .isEqualTo(0.0);

        softly.assertThat(receiverAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .doesNotContain(TransactionType.TRANSFER_IN);
    }
}