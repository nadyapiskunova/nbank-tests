package constans;

public class TestConstants {
    private TestConstants() {}

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    public static final double NEGATIVE_AMOUNT = -1.00;
    public static final double ZERO_AMOUNT = 0.00;
    public static final double MIN_AMOUNT = 0.01;

    public static final double MAX_DEPOSIT_AMOUNT = 5000.00;
    public static final double ALMOST_MAX_DEPOSIT_AMOUNT = 4999.99;
    public static final double ABOVE_MAX_DEPOSIT_AMOUNT = 5000.01;

    public static final double ALMOST_MAX_TRANSFER_AMOUNT = 9999.99;
    public static final double MAX_TRANSFER_AMOUNT = 10000.00;
    public static final double ABOVE_MAX_TRANSFER_AMOUNT = 10000.01;

    public static final Integer NON_EXISTING_ACCOUNT_ID = Integer.MAX_VALUE;
}