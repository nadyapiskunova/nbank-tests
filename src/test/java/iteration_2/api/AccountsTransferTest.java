package iteration_2.api;

import api.constans.ErrorMessages;
import api.constans.Messages;
import api.constans.TestConstants;
import api.generators.RandomData;
import api.models.*;
import api.models.comparison.ModelAssertions;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import storage.SessionStorage;

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
    @UserSession
    public void userCanTransferBetweenTheirAccountWithValidDataTest(Double amount) {
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = userSteps.createAccount();
        Integer secondAccountId = secondAccount.getId();

        repeat(2, () -> userSteps.deposit(
                firstAccountId,
                TestConstants.MAX_DEPOSIT_AMOUNT
        ));

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(amount)
                .build();
        TransferResponse transferToSecondAccountId = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK())
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

        List<AccountResponse> accountsAfterTransfer = userSteps.getAllAccounts();

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
    @UserSession(2)
    public void userCanTransferToExternalAccountWithValidDataTest(Double amount) {
        CreateUserRequest firstUser = SessionStorage.getUser(1);

        UserSteps firstUserSteps = SessionStorage.getSteps(1);
        UserSteps secondUserSteps = SessionStorage.getSteps(2);

        AccountResponse createdAccountForFirstUser = firstUserSteps.createAccount();
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = secondUserSteps.createAccount();
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        repeat(2, () -> firstUserSteps.deposit(
                accountIdByFirstUser,
                TestConstants.MAX_DEPOSIT_AMOUNT
        ));

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(amount)
                .build();
        TransferResponse transferToSecondAccountId = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authAsUser(firstUser.getUsername(), firstUser.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferToSecondUser);

        softly.assertThat(transferToSecondAccountId.getMessage()).isEqualTo(Messages.TRANSFER_SUCCESSFUL);

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 2;
        BigDecimal expectedSenderBalance = BigDecimal.valueOf(expectedBalance).subtract(BigDecimal.valueOf(amount));
        BigDecimal expectedReceiverBalance = BigDecimal.valueOf(amount);

        List<AccountResponse> accountsByFirstUserAfterTransfer = firstUserSteps.getAllAccounts();

        AccountResponse firstUserAccount = accountsByFirstUserAfterTransfer.stream()
                .filter(account -> account.getId().equals(accountIdByFirstUser))
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserAccount.getBalance())
                .isCloseTo(expectedSenderBalance.doubleValue(), within(0.001));

        softly.assertThat(firstUserAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_OUT);

        List<AccountResponse> accountsBySecondUserAfterTransfer = secondUserSteps.getAllAccounts();
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
    @UserSession
    public void userCannotTransferBetweenTheirAccountWithInvalidDataTest(Double amount, String errorValue) {
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = userSteps.createAccount();
        Integer secondAccountId = secondAccount.getId();

        repeat(3, () -> userSteps.deposit(firstAccountId, TestConstants.MAX_DEPOSIT_AMOUNT));

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(amount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = userSteps.getAllAccounts();

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
    @UserSession(2)
    public void userCannotTransferToExternalAccountWithInvalidDataTest(Double amount, String errorValue) {
        CreateUserRequest firstUser = SessionStorage.getUser(1);

        UserSteps firstUserSteps = SessionStorage.getSteps(1);
        UserSteps secondUserSteps = SessionStorage.getSteps(2);

        AccountResponse createdAccountForFirstUser = firstUserSteps.createAccount();
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = secondUserSteps.createAccount();
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        repeat(3, () -> firstUserSteps.deposit(
                accountIdByFirstUser,
                TestConstants.MAX_DEPOSIT_AMOUNT));

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(amount)
                .build();
         new CrudRequester(
                RequestSpecs.authAsUser(firstUser.getUsername(), firstUser.getPassword()),
                 Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .post(transferToSecondUser);

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 3;

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer = firstUserSteps.getAllAccounts();

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

        List<AccountResponse> accountsBySecondUserAfterFailedTransfer = secondUserSteps.getAllAccounts();
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
    @UserSession
    public void userCannotTransferAmountExceedingBalanceBetweenTheirAccountTest(){
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = userSteps.createAccount();
        Integer secondAccountId = secondAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        userSteps.deposit(firstAccountId, depositAmount);

        double transferAmount = RandomData.getAmountGreaterThan(depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER))
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = userSteps.getAllAccounts();

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
    @UserSession(2)
    public void userCannotTransferAmountExceedingBalanceToExternalAccountTest(){
        CreateUserRequest firstUser = SessionStorage.getUser(1);

        UserSteps firstUserSteps = SessionStorage.getSteps(1);
        UserSteps secondUserSteps = SessionStorage.getSteps(2);

        AccountResponse createdAccountForFirstUser = firstUserSteps.createAccount();
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = secondUserSteps.createAccount();
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        firstUserSteps.deposit(accountIdByFirstUser, depositAmount);

        double transferAmount = RandomData.getAmountGreaterThan(depositAmount);

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(transferAmount)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(firstUser.getUsername(), firstUser.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER))
                .post(transferToSecondUser);

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer = firstUserSteps.getAllAccounts();

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

        List<AccountResponse> accountsBySecondUserAfterFailedTransfer = secondUserSteps.getAllAccounts();

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
    @UserSession
    public void adminCannotTransferBetweenUserAccountsTest(){
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = userSteps.createAccount();
        Integer secondAccountId = secondAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        userSteps.deposit(firstAccountId, depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsForbidden())
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = userSteps.getAllAccounts();

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
    @UserSession(2)
    public void adminCannotTransferFromUserAccountToAnotherUsersAccountTest(){
        UserSteps firstUserSteps = SessionStorage.getSteps(1);
        UserSteps secondUserSteps = SessionStorage.getSteps(2);

        AccountResponse createdAccountForFirstUser = firstUserSteps.createAccount();
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        AccountResponse createdAccountForSecondUser = secondUserSteps.createAccount();
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        double depositAmount = RandomData.getSmallDepositAmount();

        firstUserSteps.deposit(accountIdByFirstUser, depositAmount);

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsForbidden())
                .post(transferToSecondUser);

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer = firstUserSteps.getAllAccounts();

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

        List<AccountResponse> accountsBySecondUserAfterFailedTransfer = secondUserSteps.getAllAccounts();

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
    @UserSession
    public void unauthorizedUserCannotTransferBetweenOwnAccountsTest(){
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = userSteps.createAccount();
        Integer secondAccountId = secondAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        userSteps.deposit(firstAccountId, depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsUnauthorized())
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = userSteps.getAllAccounts();

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
    @UserSession
    public void userCannotTransferToNonExistentAccountTest(){
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer firstAccountId = firstAccount.getId();

        double depositAmount = RandomData.getSmallDepositAmount();
        userSteps.deposit(firstAccountId, depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .amount(depositAmount)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER))
                .post(transferToSecondAccountIdRequest);

        List<AccountResponse> accountsAfterFailedTransfer = userSteps.getAllAccounts();

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
    @UserSession
    public void userCannotTransferFromNonExistentAccountTest() {
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse firstAccount = userSteps.createAccount();
        Integer accountId = firstAccount.getId();

        double transferAmount = RandomData.getValidDepositAmount();

        TransferRequest transferToAccountRequest = TransferRequest.builder()
                .senderAccountId(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .receiverAccountId(accountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsForbidden())
                .post(transferToAccountRequest);

        List<AccountResponse> accountsAfterFailedTransfer = userSteps.getAllAccounts();

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