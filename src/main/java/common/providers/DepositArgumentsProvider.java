package common.providers;

import api.configs.Config;
import api.constans.ErrorMessages;
import api.constans.TestConstants;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class DepositArgumentsProvider implements ArgumentsProvider {

    String minAmountError = Config.getBackendVersion().isDatabaseValidationMessages()
            ? ErrorMessages.DEPOSIT_AMOUNT_MIN_WITH_DATABASE
            : ErrorMessages.DEPOSIT_AMOUNT_MIN;

    String maxAmountError = Config.getBackendVersion().isDatabaseValidationMessages()
            ? ErrorMessages.DEPOSIT_AMOUNT_MAX_WITH_DATABASE
            : ErrorMessages.DEPOSIT_AMOUNT_MAX;

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(
                        TestConstants.NEGATIVE_AMOUNT,
                        minAmountError
                ),
                Arguments.of(
                        TestConstants.ZERO_AMOUNT,
                        minAmountError
                ),
                Arguments.of(
                        TestConstants.ABOVE_MAX_DEPOSIT_AMOUNT,
                        maxAmountError
                )
        );
    }
}