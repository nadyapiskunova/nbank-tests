package iteration_2;

import constans.ErrorMessages;
import constans.TestConstants;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.GetAccountsRequester;
import requests.DepositRequester;
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
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // создаю аккаунт
        AccountResponse createdAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                        ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountId = createdAccount.getId();

        // пользователь делает депозит на аккаунт
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        AccountResponse depositedAccount = new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest)
                .extract()
                .as(AccountResponse.class);

        // проверяем что депозит есть
        List<TransactionResponse> transactions = depositedAccount.getTransactions();

        softly.assertThat(transactions).hasSize(1);

        TransactionResponse transaction = transactions.get(0);

        softly.assertThat(transaction.getAmount()).isEqualTo(amount);
        softly.assertThat(transaction.getRelatedAccountId()).isEqualTo(accountId);
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
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // создаю аккаунт
        AccountResponse createdAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountId = createdAccount.getId();

        // пользователь делает депозит на аккаунт
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .post(depositRequest);

        // проверяем, что депозита нет
        List<AccountResponse> accountsAfterFailedDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void adminCannotDepositToUserAccountTest() {
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // создаю аккаунт
        AccountResponse createdAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountId = createdAccount.getId();

        // админ делает депозит на аккаунт
        DepositRequest adminDeposit = DepositRequest.builder()
                .id(accountId)
                .balance(RandomData.getValidDepositAmount())
                .build();

        new DepositRequester(
                RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsForbidden())
                .post(adminDeposit);

        // проверяем, что депозита нет
        List<AccountResponse> accountsAfterFailedDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void authorizedUserCannotDepositToNonExistentAccountTest(){
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // создаю аккаунт
        new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post();

        // пользователь делает депозит на не существующий аккаунт

        DepositRequest userDepositToNonExistentAccount = DepositRequest.builder()
                .id(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new DepositRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(ErrorMessages.UNAUTHORIZED_ACCESS_TO_ACCOUNT))
                .post(userDepositToNonExistentAccount);

        // проверяем, что депозита нет
        List<AccountResponse> accountsAfterFailedDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void unauthorizedUserCannotDepositToAnotherUsersAccountTest(){

        // создаем пользователя1
        CreateUserRequest firstUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();
        CreateUserResponse firstUserResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(firstUserRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(firstUserResponse.getId());

        // создаем пользователя2
        CreateUserRequest secondUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();
        CreateUserResponse secondUserResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(secondUserRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(secondUserResponse.getId());

        // создаю аккаунт для пользователя 1
         new CreateAccountRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post();

        // создаю аккаунт для пользователя 2
        AccountResponse createdAccountForSecondUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserRequest.getUsername(), secondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        // пользователь 1 делает депозит на accountIdBySecondUser
        DepositRequest userDepositToAccountIdBySecondUser = DepositRequest.builder()
                .id(accountIdBySecondUser)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new DepositRequester(RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(ErrorMessages.UNAUTHORIZED_ACCESS_TO_ACCOUNT))
                .post(userDepositToAccountIdBySecondUser);

        // проверяем, что депозита нет
        List<AccountResponse> accountsAfterFailedDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(secondUserRequest.getUsername(), secondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

        AccountResponse accountAfterFailedDeposit = accountsAfterFailedDeposit.get(0);

        softly.assertThat(accountAfterFailedDeposit.getBalance()).isEqualTo(0.0);
        softly.assertThat(accountAfterFailedDeposit.getTransactions()).isEmpty();
    }

    @Test
    public void unauthorizedUserCannotDepositFundsToAccountTest(){
        // создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse userResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest)
                .extract()
                .as(CreateUserResponse.class);
        createdUserIds.add(userResponse.getId());

        // создаю аккаунт
        AccountResponse createdAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountId = createdAccount.getId();

        // пользователь делает депозит
        DepositRequest unauthorizedUserDeposit = DepositRequest.builder()
                .id(accountId)
                .balance(RandomData.getValidDepositAmount())
                .build();
        new DepositRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(unauthorizedUserDeposit);
    }
}
