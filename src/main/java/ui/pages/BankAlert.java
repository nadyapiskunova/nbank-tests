package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),
    SUCCESSFULLY_DEPOSITED("✅ Successfully deposited"),
    PLEASE_ENTER_VALID_AMOUNT("❌ Please enter a valid amount."),
    PLEASE_SELECT_ACCOUNT("❌ Please select an account."),
    SUCCESSFULLY_TRANSFERRED("✅ Successfully transferred "),
    TRANSFER_AMOUNT_MUST_BE_AT_LEAST_0_01("❌ Error: Transfer amount must be at least 0.01"),
    PLEASE_FILL_ALL_FIELDS_AND_CONFIRM("❌ Please fill all fields and confirm."),
    NO_MATCHING_USERS_FOUND("❌ No matching users found."),
    TRANSFER_OF_SUCCESSFULLY("✅ Transfer of"),
    NAME_UPDATE_SUCCESSFULLY("✅ Name updated successfully!"),
    NAME_MUST_CONTAIN_TWO_WORDS_WITH_LETTERS_ONLY("Name must contain two words with letters only"),
    PLEASE_ENTER_VALID_NAME("❌ Please enter a valid name.");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}
