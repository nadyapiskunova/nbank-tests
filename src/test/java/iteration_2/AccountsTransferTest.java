package iteration_2;

import constans.ErrorMessages;
import constans.Messages;
import constans.TestConstants;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
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
        AccountResponse firstAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer secondAccountId = secondAccount.getId();

        // пользователь делает 2 раза депозит на аккаунт
        DepositRequest depositRequest = DepositRequest.builder()
                .id(firstAccountId)
                .balance(TestConstants.MAX_DEPOSIT_AMOUNT)
                .build();

        DepositRequester depositRequester = new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK());
        repeat(2, () -> depositRequester.post(depositRequest));

        // пользователь делает трансфер с firstAccountId на secondAccountId
        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(amount)
                .build();

        TransferResponse transferToSecondAccountId = new TransferRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferToSecondAccountIdRequest)
                .extract()
                .as(TransferResponse.class);

        softly.assertThat(transferToSecondAccountId.getMessage()).isEqualTo(Messages.TRANSFER_SUCCESSFUL);
        softly.assertThat(transferToSecondAccountId.getAmount()).isEqualTo(amount);
        softly.assertThat(transferToSecondAccountId.getReceiverAccountId()).isEqualTo(secondAccountId);
        softly.assertThat(transferToSecondAccountId.getSenderAccountId()).isEqualTo(firstAccountId);


        // проверяю состояние счетов
        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 2;

        BigDecimal expectedSenderBalance = BigDecimal.valueOf(expectedBalance)
                .subtract(BigDecimal.valueOf(amount));
        BigDecimal expectedReceiverBalance = BigDecimal.valueOf(amount);

        List<AccountResponse> accountsAfterTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

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
        AccountResponse createdAccountForFirstUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        // создаю аккаунт для пользователя 2
        AccountResponse createdAccountForSecondUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserRequest.getUsername(), secondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        // пользователь1 делает 2 раза депозит на аккаунт
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdByFirstUser)
                .balance(TestConstants.MAX_DEPOSIT_AMOUNT)
                .build();

        DepositRequester depositRequester = new DepositRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK());
        repeat(2, () -> depositRequester.post(depositRequest));

        // юзер 1 делает трансфер юзеру 2
        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(amount)
                .build();

        TransferResponse transferToSecondAccountId = new TransferRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferToSecondUser)
                .extract()
                .as(TransferResponse.class);
        softly.assertThat(transferToSecondAccountId.getMessage()).isEqualTo(Messages.TRANSFER_SUCCESSFUL);
        softly.assertThat(transferToSecondAccountId.getAmount()).isEqualTo(amount);
        softly.assertThat(transferToSecondAccountId.getReceiverAccountId()).isEqualTo(accountIdBySecondUser);
        softly.assertThat(transferToSecondAccountId.getSenderAccountId()).isEqualTo(accountIdByFirstUser);

        // проверяю состояние счетов
        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 2;

        BigDecimal expectedSenderBalance = BigDecimal.valueOf(expectedBalance).subtract(BigDecimal.valueOf(amount));
        BigDecimal expectedReceiverBalance = BigDecimal.valueOf(amount);

        // счет юзера 1
        List<AccountResponse> accountsByFirstUserAfterTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                firstUserRequest.getUsername(),
                                firstUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

        AccountResponse firstUserAccount = accountsByFirstUserAfterTransfer.stream()
                .filter(account -> account.getId().equals(accountIdByFirstUser))
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserAccount.getBalance())
                .isCloseTo(expectedSenderBalance.doubleValue(), within(0.001));

        softly.assertThat(firstUserAccount.getTransactions())
                .extracting(TransactionResponse::getType)
                .contains(TransactionType.TRANSFER_OUT);

        // счет юзера 2
        List<AccountResponse> accountsBySecondUserAfterTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                secondUserRequest.getUsername(),
                                secondUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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
        AccountResponse firstAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer secondAccountId = secondAccount.getId();

        // пользователь делает 3 раза депозит на аккаунт
        DepositRequest depositRequest = DepositRequest.builder()
                .id(firstAccountId)
                .balance(TestConstants.MAX_DEPOSIT_AMOUNT)
                .build();

        DepositRequester depositRequester = new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK());
        repeat(3, () -> depositRequester.post(depositRequest));

        // пользователь делает трансфер с firstAccountId на secondAccountId
        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(amount)
                .build();

         new TransferRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .post(transferToSecondAccountIdRequest);

        // проверяю, что трансфера не было
        List<AccountResponse> accountsAfterFailedTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

        AccountResponse senderAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(firstAccountId))
                .findFirst()
                .orElseThrow();

        AccountResponse receiverAccount = accountsAfterFailedTransfer.stream()
                .filter(account -> account.getId().equals(secondAccountId))
                .findFirst()
                .orElseThrow();

        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 3;
        // счет отправителя не изменился
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
        AccountResponse createdAccountForFirstUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        // создаю аккаунт для пользователя 2
        AccountResponse createdAccountForSecondUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserRequest.getUsername(), secondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        // пользователь1 делает 3 раза депозит на аккаунт
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdByFirstUser)
                .balance(TestConstants.MAX_DEPOSIT_AMOUNT)
                .build();

        DepositRequester depositRequester = new DepositRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK());
        repeat(3, () -> depositRequester.post(depositRequest));

        // юзер 1 делает трансфер юзеру 2
        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(amount)
                .build();
         new TransferRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(errorValue))
                .post(transferToSecondUser);

        // проверяю, что трансфера не было
        // проверяем счет юзера 1
        double expectedBalance = TestConstants.MAX_DEPOSIT_AMOUNT * 3;

        List<AccountResponse> accountsByFirstUserAfterFailedTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                firstUserRequest.getUsername(),
                                firstUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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

        // проверяем счет юзера 2
        List<AccountResponse> accountsBySecondUserAfterFailedTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                secondUserRequest.getUsername(),
                                secondUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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
        AccountResponse firstAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer secondAccountId = secondAccount.getId();

        // пользователь делает депозит на аккаунт
        double depositAmount = RandomData.getSmallDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(firstAccountId)
                .balance(depositAmount)
                .build();
        new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        // пользователь делает трансфер с firstAccountId на secondAccountId
        double transferAmount = RandomData.getAmountGreaterThan(depositAmount);

        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER))
                .post(transferToSecondAccountIdRequest);

        // проверяю, что трансфера не было
        List<AccountResponse> accountsAfterFailedTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

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
        AccountResponse createdAccountForFirstUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        // создаю аккаунт для пользователя 2
        AccountResponse createdAccountForSecondUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserRequest.getUsername(), secondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        // пользователь 1 делает депозит на свой счет
        double depositAmount = RandomData.getSmallDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdByFirstUser)
                .balance(depositAmount)
                .build();
        new DepositRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        // юзер 1 делает трансфер юзеру 2
        double transferAmount = RandomData.getAmountGreaterThan(depositAmount);

        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(transferAmount)
                .build();
        new TransferRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER))
                .post(transferToSecondUser);

        // проверяю, что трансфера не было
        // проверяем счет юзера 1
        List<AccountResponse> accountsByFirstUserAfterFailedTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                firstUserRequest.getUsername(),
                                firstUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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


        // проверяем счет юзера 2
        List<AccountResponse> accountsBySecondUserAfterFailedTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                secondUserRequest.getUsername(),
                                secondUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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
        AccountResponse firstAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer secondAccountId = secondAccount.getId();

        // пользователь делает депозит на аккаунт
        double depositAmount = RandomData.getSmallDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(firstAccountId)
                .balance(depositAmount)
                .build();
        new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        // админ делает трансфер с firstAccountId на secondAccountId
        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(depositAmount)
                .build();

        new TransferRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsForbidden())
                .post(transferToSecondAccountIdRequest);

        // проверяю, что трансфера не было
        List<AccountResponse> accountsAfterFailedTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

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
        AccountResponse createdAccountForFirstUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdByFirstUser = createdAccountForFirstUser.getId();

        // создаю аккаунт для пользователя 2
        AccountResponse createdAccountForSecondUser = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserRequest.getUsername(), secondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountIdBySecondUser = createdAccountForSecondUser.getId();

        // пользователь 1 делает депозит на свой счет
        double depositAmount = RandomData.getSmallDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdByFirstUser)
                .balance(depositAmount)
                .build();
        new DepositRequester(
                RequestSpecs.authAsUser(firstUserRequest.getUsername(), firstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        // админ делает трансфер со счета юзера 1 на счет юзеру 2
        TransferRequest transferToSecondUser = TransferRequest.builder()
                .senderAccountId(accountIdByFirstUser)
                .receiverAccountId(accountIdBySecondUser)
                .amount(depositAmount)
                .build();
        new TransferRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsForbidden())
                .post(transferToSecondUser);

        // проверяю, что трансфера не было
        // проверяем счет юзера 1
        List<AccountResponse> accountsByFirstUserAfterFailedTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                firstUserRequest.getUsername(),
                                firstUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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


// проверяем счет юзера 2
        List<AccountResponse> accountsBySecondUserAfterFailedTransfer =
                new GetAccountsRequester(
                        RequestSpecs.authAsUser(
                                secondUserRequest.getUsername(),
                                secondUserRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK())
                        .getAsList();

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
        AccountResponse firstAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer firstAccountId = firstAccount.getId();

        AccountResponse secondAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer secondAccountId = secondAccount.getId();

        // пользователь делает депозит на аккаунт
        double depositAmount = RandomData.getSmallDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(firstAccountId)
                .balance(depositAmount)
                .build();
         new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        // неавторизованный юзер делает трансфер с firstAccountId на secondAccountId
        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(depositAmount)
                .build();

        new TransferRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(transferToSecondAccountIdRequest);

        // проверяю, что трансфера не было
        List<AccountResponse> accountsAfterFailedTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

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
        AccountResponse firstAccount = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer firstAccountId = firstAccount.getId();

        // пользователь делает депозит на аккаунт
        double depositAmount = RandomData.getSmallDepositAmount();
        DepositRequest depositRequest = DepositRequest.builder()
                .id(firstAccountId)
                .balance(depositAmount)
                .build();
        new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        // пользователь делает трансфер с firstAccountId на nonExistingAccountId
        TransferRequest transferToSecondAccountIdRequest = TransferRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .amount(depositAmount)
                .build();

        new TransferRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(ErrorMessages.INVALID_TRANSFER))
                .post(transferToSecondAccountIdRequest);

        // проверяю, что трансфера не было
        List<AccountResponse> accountsAfterFailedTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

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
        AccountResponse account = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(AccountResponse.class);
        Integer accountId = account.getId();

        // пользователь делает трансфер с nonExistingAccountId на accountId
        double transferAmount = RandomData.getValidDepositAmount();

        TransferRequest transferToAccountRequest = TransferRequest.builder()
                .senderAccountId(TestConstants.NON_EXISTING_ACCOUNT_ID)
                .receiverAccountId(accountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden())
                .post(transferToAccountRequest);

        // проверяю, что трансфера не было
        List<AccountResponse> accountsAfterFailedTransfer = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAsList();

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