package iteration_2.api;

import api.constans.ErrorMessages;
import api.constans.TestConstants;
import api.dao.AccountDao;
import api.dao.TransactionDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.generators.RandomData;
import api.models.*;
import api.models.comparison.ModelAssertions;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.DepositInvalidArguments;
import common.annotations.UserSession;
import common.helpers.DbCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import storage.SessionStorage;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AccountsDepositTest extends BaseTest {

    public static Stream<Arguments> validDataForUserCanDepositWithValidDataTest(){

        return Stream.of(
                Arguments.of(TestConstants.MIN_AMOUNT),
                Arguments.of(TestConstants.ALMOST_MAX_DEPOSIT_AMOUNT),
                Arguments.of(TestConstants.MAX_DEPOSIT_AMOUNT)
        );
    }

    @MethodSource("validDataForUserCanDepositWithValidDataTest")
    @ParameterizedTest
    @UserSession
    public void userCanDepositWithValidDataTest(Double amount) {
        UserSteps userSteps = SessionStorage.getSteps();
        CreateUserRequest user = SessionStorage.getUser();

        AccountResponse account = userSteps.createAccount();
        Integer accountId = account.getId();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
        AccountResponse depositedAccount = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        List<TransactionResponse> transactions = depositedAccount.getTransactions();
        TransactionResponse transaction = transactions.get(0);

        softly.assertThat(transactions).hasSize(1);
        ModelAssertions.assertThatModels(depositRequest, transaction).match();

        DbCheck.run(() -> {
            AccountDao accountDao = DataBaseSteps.getAccountById(accountId);
            TransactionDao transactionDao = DataBaseSteps.getTransactionByAccountId(accountId);

            assertThat(accountDao.getBalance()).isEqualTo(amount);

            DaoAndModelAssertions
                    .assertThat(depositRequest, transactionDao)
                    .match();

            assertThat(transactionDao.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(transactionDao.getRelatedAccountId()).isEqualTo(accountId);
        });
    }

    @DepositInvalidArguments
    @ParameterizedTest
    @UserSession
    public void userCannotDepositWithInvalidAmountTest(Double amount, String errorValue) {
        UserSteps userSteps = SessionStorage.getSteps();

        CreateUserRequest user = SessionStorage.getUser();
        AccountResponse account = userSteps.createAccount();
        Integer accountId = account.getId();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(),user.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .post(depositRequest);

        List<AccountResponse> accountsAfterFailedDeposit = userSteps.getAllAccounts();
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();

        DbCheck.run(() -> {
            AccountDao accountDao = DataBaseSteps.getAccountById(accountId);

            assertThat(accountDao.getBalance()).isEqualTo(0.0);
            assertNull(DataBaseSteps.getTransactionByAccountId(accountId));
        });
    }

    @Test
    @UserSession
    public void adminCannotDepositToUserAccountTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse account = userSteps.createAccount();
        Integer accountId = account.getId();

        DepositRequest adminDeposit = DepositRequest.builder()
                .id(accountId)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden())
                .post(adminDeposit);

        List<AccountResponse> accountsAfterFailedDeposit = userSteps.getAllAccounts();
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();

        DbCheck.run(() -> {
            AccountDao accountDao = DataBaseSteps.getAccountById(accountId);

            assertThat(accountDao.getBalance()).isEqualTo(0.0);
            assertNull(DataBaseSteps.getTransactionByAccountId(accountId));
        });
    }

    @Test
    @UserSession
    public void authorizedUserCannotDepositToNonExistentAccountTest(){
        UserSteps userSteps = SessionStorage.getSteps();

        CreateUserRequest user = SessionStorage.getUser();
        userSteps.createAccount();

        AccountResponse nonExistentAccount = userSteps.createAccount();
        Integer nonExistentAccountId = nonExistentAccount.getId();

        DepositRequest userDepositToNonExistentAccount = DepositRequest.builder()
                .id(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(),user.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(ErrorMessages.UNAUTHORIZED_ACCESS_TO_ACCOUNT))
                .post(userDepositToNonExistentAccount);

        List<AccountResponse> accountsAfterFailedDeposit = userSteps.getAllAccounts();
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();

        DbCheck.run(() ->
                assertNull(
                        DataBaseSteps.getTransactionByAccountId(nonExistentAccountId)
                )
        );
    }

    @Test
    @UserSession(2)
    public void authorizedUserCannotDepositToAnotherUsersAccountTest(){
       CreateUserRequest firstUser = SessionStorage.getUser(1);

       UserSteps firstUserSteps = SessionStorage.getSteps(1);
       UserSteps secondUserSteps = SessionStorage.getSteps(2);

       firstUserSteps.createAccount();

       AccountResponse createdAccountForSecondUser = secondUserSteps.createAccount();
       Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdBySecondUser)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(firstUser.getUsername(),firstUser.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(ErrorMessages.UNAUTHORIZED_ACCESS_TO_ACCOUNT))
                .post(depositRequest);

        List<AccountResponse> accountsAfterFailedDeposit = secondUserSteps.getAllAccounts();
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();

        DbCheck.run(() ->
                assertNull(
                        DataBaseSteps.getTransactionByAccountId(accountIdBySecondUser)
                )
        );
    }

    @Test
    @UserSession
    public void unauthorizedUserCannotDepositFundsToAccountTest(){
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse account = userSteps.createAccount();
        Integer accountId = account.getId();

        DepositRequest unauthorizedUserDeposit = DepositRequest.builder()
                .id(accountId)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsUnauthorized())
                .post(unauthorizedUserDeposit);

        DbCheck.run(() ->
                assertNull(
                        DataBaseSteps.getTransactionByAccountId(accountId)
                )
        );
    }
}