package common.helpers;

import api.configs.Config;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountHelper {

    public static double expectedBalance(double value) {
        if (!Config.getBackendVersion().isBalanceRounding()) {
            return value;
        }

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}