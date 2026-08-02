package common.testdata;

public final class FraudCheckTestData {

    private FraudCheckTestData() {
    }

    public static final String FRAUD_CHECK_STATUS_SUCCESS = "SUCCESS";

    public static final String FRAUD_CHECK_DECISION_APPROVED = "APPROVED";
    public static final String FRAUD_CHECK_DECISION_REJECTED = "REJECTED";

    public static final String TRANSFER_STATUS_APPROVED = "APPROVED";
    public static final String TRANSFER_STATUS_MANUAL_REVIEW = "MANUAL_REVIEW_REQUIRED";
    public static final String TRANSFER_STATUS_REJECTED = "REJECTED";

    public static final double LOW_RISK_SCORE = 0.2;
    public static final double HIGH_RISK_SCORE = 0.9;

    public static final String LOW_RISK_REASON = "Low risk transaction";
    public static final String HIGH_RISK_REASON = "High risk transaction";

    public static final String TRANSFER_APPROVED_MESSAGE =
            "Transfer approved and processed immediately";

    public static final String TRANSFER_MANUAL_REVIEW_MESSAGE =
            "Transfer requires manual review";

    public static final String TRANSFER_REJECTED_MESSAGE =
            "Transfer rejected due to fraud risk";

    public static final String FRAUD_CHECK_DECISION_MANUAL_REVIEW = "MANUAL_REVIEW_REQUIRED";
}