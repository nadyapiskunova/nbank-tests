package api.constans;

public class ErrorMessages {
    private ErrorMessages() {}

    public static final String USERNAME_BLANK =
            "Username cannot be blank";

    public static final String USERNAME_LENGTH =
            "Username must be between 3 and 15 characters";

    public static final String USERNAME_INVALID_CHARS =
            "Username must contain only letters, digits, dashes, underscores, and dots";

    public static final String DEPOSIT_AMOUNT_MIN =
            "Deposit amount must be at least 0.01";

    public static final String DEPOSIT_AMOUNT_MAX =
            "Deposit amount cannot exceed 5000";

    public static final String UNAUTHORIZED_ACCESS_TO_ACCOUNT =
            "Unauthorized access to account";

    public static final String TRANSFER_AMOUNT_MIN =
            "Transfer amount must be at least 0.01";

    public static final String TRANSFER_AMOUNT_MAX =
            "Transfer amount cannot exceed 10000";

    public static final String INVALID_TRANSFER =
            "Invalid transfer: insufficient funds or invalid accounts";

    public static final String INVALID_NAME =
            "Name must contain two words with letters only";
}