package api.generators;

import api.constans.TestConstants;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public class RandomData {
    private RandomData(){}

    private static final String SPECIAL_CHARACTERS =
            "!@#$%^&*()_+-=[]{}|;:'\",.<>?/\\`~";

    public static String getUsername(){
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getPassword(){
        return RandomStringUtils.randomAlphabetic(3).toUpperCase() +
                RandomStringUtils.randomAlphabetic(5).toLowerCase() +
                RandomStringUtils.randomNumeric(3) + "%$#";
    }

    public static String getDigit() {
        return RandomStringUtils.randomNumeric(1);
    }

    public static String getSpecialCharacter() {
        return RandomStringUtils.random(1, SPECIAL_CHARACTERS);
    }

    public static String getValidName() {
        return RandomStringUtils.randomAlphabetic(5, 16)
                + " "
                + RandomStringUtils.randomAlphabetic(5, 16);
    }

    public static String getMaxLengthName() {
        return RandomStringUtils.randomAlphabetic(50)
                + " "
                + RandomStringUtils.randomAlphabetic(50);
    }

    public static String getMinLengthName() {
        return RandomStringUtils.randomAlphabetic(1)
                + " "
                + RandomStringUtils.randomAlphabetic(1);
    }

    public static String getNameWithoutSurname() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getNameWithDigits() {
        return RandomStringUtils.randomAlphabetic(5)
                + getDigit()
                + " "
                + RandomStringUtils.randomAlphabetic(5);
    }

    public static String getNameWithSpecialCharacter() {
        return RandomStringUtils.randomAlphabetic(5)
                + " "
                + RandomStringUtils.randomAlphabetic(5)
                + getSpecialCharacter();
    }

    public static double getValidDepositAmount() {
        return BigDecimal.valueOf(
                        ThreadLocalRandom.current().nextDouble(
                                TestConstants.MIN_AMOUNT,
                                TestConstants.MAX_DEPOSIT_AMOUNT + 0.01))
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    public static double getSmallDepositAmount() {
        return ThreadLocalRandom.current()
                .nextDouble(10.0, 100.0);
    }

    public static double getTransferAmount() {
        return ThreadLocalRandom.current()
                .nextDouble(0.01, 1999.99);
    }

    public static double getAmountGreaterThan(double amount) {
        return BigDecimal.valueOf(
                        ThreadLocalRandom.current()
                                .nextDouble(amount + 1, amount * 10))
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }
}
