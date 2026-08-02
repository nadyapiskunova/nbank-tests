package common.providers;

import api.configs.Config;
import api.constans.ErrorMessages;
import api.constans.TestConstants;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class TransferArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

        String minAmountError = Config.getBackendVersion().isDatabaseValidationMessages()
                ? ErrorMessages.TRANSFER_AMOUNT_MIN_WITH_DATABASE
                : ErrorMessages.TRANSFER_AMOUNT_MIN;

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
                        TestConstants.ABOVE_MAX_TRANSFER_AMOUNT,
                        ErrorMessages.TRANSFER_AMOUNT_MAX
                )
        );
    }
}