package iteration_2.api;

import api.constans.ErrorMessages;
import api.constans.TestConstants;
import api.generators.RandomData;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.models.DepositRequest;
import api.models.TransactionResponse;
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

import java.util.List;
import java.util.stream.Stream;

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
    }

    public static Stream<Arguments> dataForUserCannotDepositWithInvalidAmountTest(){

        return Stream.of(
                Arguments.of(TestConstants.NEGATIVE_AMOUNT, ErrorMessages.DEPOSIT_AMOUNT_MIN),
                Arguments.of(TestConstants.ZERO_AMOUNT, ErrorMessages.DEPOSIT_AMOUNT_MIN),
                Arguments.of(TestConstants.ABOVE_MAX_DEPOSIT_AMOUNT, ErrorMessages.DEPOSIT_AMOUNT_MAX)
        );
    }

    @MethodSource("dataForUserCannotDepositWithInvalidAmountTest")
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
    }

    @Test
    @UserSession
    public void authorizedUserCannotDepositToNonExistentAccountTest(){
        UserSteps userSteps = SessionStorage.getSteps();

        CreateUserRequest user = SessionStorage.getUser();
        userSteps.createAccount();

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
    }
}