package iteration_2.api;

import constans.ErrorMessages;
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
    public void userCanDepositWithValidDataTest(Double amount) {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse account = UserSteps.createAccount(userRequest);
        Integer accountId = account.getId();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
        AccountResponse depositedAccount = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(), Endpoint.DEPOSIT)
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
    public void userCannotDepositWithInvalidAmountTest(Double amount, String errorValue) {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse account = UserSteps.createAccount(userRequest);
        Integer accountId = account.getId();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(), Endpoint.DEPOSIT)
                .post(depositRequest);

        List<AccountResponse> accountsAfterFailedDeposit = UserSteps.getAccounts(userRequest);
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void adminCannotDepositToUserAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse account = UserSteps.createAccount(userRequest);
        Integer accountId = account.getId();

        DepositRequest adminDeposit = DepositRequest.builder()
                .id(accountId)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsForbidden(), Endpoint.DEPOSIT)
                .post(adminDeposit);

        List<AccountResponse> accountsAfterFailedDeposit = UserSteps.getAccounts(userRequest);
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void authorizedUserCannotDepositToNonExistentAccountTest(){
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        UserSteps.createAccount(userRequest);

        DepositRequest userDepositToNonExistentAccount = DepositRequest.builder()
                .id(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(ErrorMessages.UNAUTHORIZED_ACCESS_TO_ACCOUNT), Endpoint.DEPOSIT)
                .post(userDepositToNonExistentAccount);

        List<AccountResponse> accountsAfterFailedDeposit = UserSteps.getAccounts(userRequest);
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void authorizedUserCannotDepositToAnotherUsersAccountTest(){
        CreateUserRequest firstUserRequest = AdminSteps.createUser(createdUserIds);

        CreateUserRequest secondUserRequest = AdminSteps.createUser(createdUserIds);

        UserSteps.createAccount(firstUserRequest);

        AccountResponse createdAccountForSecondUser = UserSteps.createAccount(secondUserRequest);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdBySecondUser)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(),firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(ErrorMessages.UNAUTHORIZED_ACCESS_TO_ACCOUNT), Endpoint.DEPOSIT)
                .post(depositRequest);

        List<AccountResponse> accountsAfterFailedDeposit = UserSteps.getAccounts(secondUserRequest);
        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void unauthorizedUserCannotDepositFundsToAccountTest(){
        CreateUserRequest userRequest = AdminSteps.createUser(createdUserIds);

        AccountResponse account = UserSteps.createAccount(userRequest);
        Integer accountId = account.getId();

        DepositRequest unauthorizedUserDeposit = DepositRequest.builder()
                .id(accountId)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized(), Endpoint.DEPOSIT)
                .post(unauthorizedUserDeposit);
    }
}
