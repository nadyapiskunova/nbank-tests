package iteration_2.api;

import api.contract.BackendVersion;
import api.dao.AccountDao;
import api.dao.TransactionDao;
import api.generators.RandomData;
import api.models.AccountResponse;
import api.models.FraudCheckTransferResponse;
import api.models.TransactionType;
import api.models.comparison.ModelAssertions;
import api.requests.steps.DataBaseSteps;
import api.requests.steps.UserSteps;
import common.annotations.APIVersion;
import common.annotations.FraudCheckMock;
import common.annotations.UserSession;
import common.extensions.FraudCheckWireMockExtension;
import common.testdata.FraudCheckTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import storage.SessionStorage;

@APIVersion(BackendVersion.WITH_FRAUD_CHECK_WITH_TRANSFER_FIX)
@ExtendWith(FraudCheckWireMockExtension.class)
@ResourceLock("fraud-check-wiremock")
public class TransferWithFraudTest extends BaseTest {

    @Test
    @UserSession
    @FraudCheckMock(
            status = FraudCheckTestData.FRAUD_CHECK_STATUS_SUCCESS,
            decision = FraudCheckTestData.FRAUD_CHECK_DECISION_APPROVED,
            riskScore = FraudCheckTestData.LOW_RISK_SCORE,
            reason = FraudCheckTestData.LOW_RISK_REASON,
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void userCanTransferWhenFraudCheckApprovedTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse senderAccount = userSteps.createAccount();
        AccountResponse receiverAccount = userSteps.createAccount();

        double balance = RandomData.getTransferBalance();
        double transferAmount = RandomData.getValidTransferAmount(balance);

        DataBaseSteps.updateAccountBalance(
                senderAccount.getId(),
                balance
        );

        FraudCheckTransferResponse actualResponse =
                userSteps.transferWithFraudCheck(
                        senderAccount.getId(),
                        receiverAccount.getId(),
                        transferAmount
                );

        FraudCheckTransferResponse expectedResponse =
                FraudCheckTransferResponse.builder()
                        .status(FraudCheckTestData.TRANSFER_STATUS_APPROVED)
                        .message(FraudCheckTestData.TRANSFER_APPROVED_MESSAGE)
                        .amount(transferAmount)
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .fraudRiskScore(FraudCheckTestData.LOW_RISK_SCORE)
                        .fraudReason(FraudCheckTestData.LOW_RISK_REASON)
                        .requiresManualReview(false)
                        .requiresVerification(false)
                        .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
        TransactionDao actualTransaction =
                DataBaseSteps.getTransactionByAccountIdAndType(
                        senderAccount.getId(),
                        TransactionType.TRANSFER_OUT
                );

        softly.assertThat(actualTransaction.getAmount()).isEqualTo(transferAmount);
        softly.assertThat(actualTransaction.getRelatedAccountId()).isEqualTo(receiverAccount.getId());

        AccountDao actualSenderAccount = DataBaseSteps.getAccountById(senderAccount.getId());
        AccountDao actualReceiverAccount = DataBaseSteps.getAccountById(receiverAccount.getId());

        softly.assertThat(actualSenderAccount.getBalance()).isEqualTo(balance - transferAmount);

        softly.assertThat(actualReceiverAccount.getBalance()).isEqualTo(transferAmount);
    }

    @Test
    @UserSession
    @FraudCheckMock(
            status = FraudCheckTestData.FRAUD_CHECK_STATUS_SUCCESS,
            decision = FraudCheckTestData.FRAUD_CHECK_DECISION_MANUAL_REVIEW,
            riskScore = FraudCheckTestData.HIGH_RISK_SCORE,
            reason = FraudCheckTestData.HIGH_RISK_REASON,
            requiresManualReview = true,
            additionalVerificationRequired = false
    )
    public void userShouldReceiveManualReviewStatusWhenFraudCheckRequiresManualReviewTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse senderAccount = userSteps.createAccount();
        AccountResponse receiverAccount = userSteps.createAccount();

        double balance = RandomData.getTransferBalance();
        double transferAmount = RandomData.getValidTransferAmount(balance);

        DataBaseSteps.updateAccountBalance(
                senderAccount.getId(),
                balance
        );

        FraudCheckTransferResponse actualResponse =
                userSteps.transferWithFraudCheck(
                        senderAccount.getId(),
                        receiverAccount.getId(),
                        transferAmount
                );

        FraudCheckTransferResponse expectedResponse =
                FraudCheckTransferResponse.builder()
                        .status(FraudCheckTestData.TRANSFER_STATUS_MANUAL_REVIEW)
                        .message(FraudCheckTestData.TRANSFER_MANUAL_REVIEW_MESSAGE)
                        .amount(transferAmount)
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .fraudRiskScore(FraudCheckTestData.HIGH_RISK_SCORE)
                        .fraudReason(FraudCheckTestData.HIGH_RISK_REASON)
                        .requiresManualReview(true)
                        .requiresVerification(false)
                        .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();

        AccountDao actualSenderAccount = DataBaseSteps.getAccountById(senderAccount.getId());
        AccountDao actualReceiverAccount = DataBaseSteps.getAccountById(receiverAccount.getId());

        softly.assertThat(actualSenderAccount.getBalance()).isEqualTo(balance);
        softly.assertThat(actualReceiverAccount.getBalance()).isZero();

        TransactionDao actualTransaction =
                DataBaseSteps.getTransactionByAccountIdAndType(
                        senderAccount.getId(),
                        TransactionType.TRANSFER_OUT
                );

        softly.assertThat(actualTransaction).isNull();
    }

    @Test
    @UserSession
    @FraudCheckMock(
            status = FraudCheckTestData.FRAUD_CHECK_STATUS_SUCCESS,
            decision = FraudCheckTestData.FRAUD_CHECK_DECISION_REJECTED,
            riskScore = FraudCheckTestData.HIGH_RISK_SCORE,
            reason = FraudCheckTestData.HIGH_RISK_REASON,
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void userShouldReceiveRejectedStatusWhenFraudCheckRejectsTransferTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse senderAccount = userSteps.createAccount();
        AccountResponse receiverAccount = userSteps.createAccount();

        double balance = RandomData.getTransferBalance();
        double transferAmount = RandomData.getValidTransferAmount(balance);

        DataBaseSteps.updateAccountBalance(
                senderAccount.getId(),
                balance
        );

        FraudCheckTransferResponse actualResponse =
                userSteps.transferWithFraudCheck(
                        senderAccount.getId(),
                        receiverAccount.getId(),
                        transferAmount
                );

        FraudCheckTransferResponse expectedResponse =
                FraudCheckTransferResponse.builder()
                        .status(FraudCheckTestData.TRANSFER_STATUS_REJECTED)
                        .message(FraudCheckTestData.TRANSFER_REJECTED_MESSAGE)
                        .amount(transferAmount)
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .fraudRiskScore(FraudCheckTestData.HIGH_RISK_SCORE)
                        .fraudReason(FraudCheckTestData.HIGH_RISK_REASON)
                        .requiresManualReview(false)
                        .requiresVerification(false)
                        .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
        AccountDao actualSenderAccount = DataBaseSteps.getAccountById(senderAccount.getId());
        AccountDao actualReceiverAccount = DataBaseSteps.getAccountById(receiverAccount.getId());

        softly.assertThat(actualSenderAccount.getBalance()).isEqualTo(balance);

        softly.assertThat(actualReceiverAccount.getBalance()).isZero();

        TransactionDao actualTransaction =
                DataBaseSteps.getTransactionByAccountIdAndType(
                        senderAccount.getId(),
                        TransactionType.TRANSFER_OUT
                );

        softly.assertThat(actualTransaction).isNull();
    }

    @Test
    @UserSession
    @FraudCheckMock(
            status = FraudCheckTestData.FRAUD_CHECK_STATUS_SUCCESS,
            decision = FraudCheckTestData.FRAUD_CHECK_DECISION_APPROVED,
            riskScore = FraudCheckTestData.LOW_RISK_SCORE,
            reason = FraudCheckTestData.LOW_RISK_REASON,
            requiresManualReview = false,
            additionalVerificationRequired = true
    )
    public void userShouldReceiveVerificationRequiredFlagWhenFraudCheckRequiresVerificationTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        AccountResponse senderAccount = userSteps.createAccount();
        AccountResponse receiverAccount = userSteps.createAccount();

        double balance = RandomData.getTransferBalance();
        double transferAmount = RandomData.getValidTransferAmount(balance);

        DataBaseSteps.updateAccountBalance(
                senderAccount.getId(),
                balance
        );

        FraudCheckTransferResponse actualResponse =
                userSteps.transferWithFraudCheck(
                        senderAccount.getId(),
                        receiverAccount.getId(),
                        transferAmount
                );

        FraudCheckTransferResponse expectedResponse =
                FraudCheckTransferResponse.builder()
                        .status(FraudCheckTestData.TRANSFER_STATUS_APPROVED)
                        .message(FraudCheckTestData.TRANSFER_APPROVED_MESSAGE)
                        .amount(transferAmount)
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .fraudRiskScore(FraudCheckTestData.LOW_RISK_SCORE)
                        .fraudReason(FraudCheckTestData.LOW_RISK_REASON)
                        .requiresManualReview(false)
                        .requiresVerification(true)
                        .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }
}