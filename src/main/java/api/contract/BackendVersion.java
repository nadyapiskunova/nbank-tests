package api.contract;
import lombok.Getter;

@Getter
public enum BackendVersion {

    WITH_VALIDATION_FIX(
            false,
            false,
            false
    ),

    WITH_DATABASE_WITH_FIX(
            true,
            true,
            true
    );

    private final boolean databaseSupported;
    private final boolean databaseValidationMessages;
    private final boolean balanceRounding;

    BackendVersion(boolean databaseSupported, boolean databaseValidationMessages, boolean balanceRounding) {
        this.databaseSupported = databaseSupported;
        this.databaseValidationMessages = databaseValidationMessages;
        this.balanceRounding = balanceRounding;
    }
}