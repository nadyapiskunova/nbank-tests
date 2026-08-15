package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class DepositPage extends BasePage<DepositPage> {
    private SelenideElement depositButton =
            $(Selectors.byText("💵 Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage clickDeposit() {
        depositButton.click();
        return this;
    }

    public DepositPage makeDeposit(
            double amount,
            String expectedAlertMessage
    ) {
        return StepLogger.ui(
                "Make deposit: " + amount,
                () -> {
                    setAmount(amount);
                    clickDeposit();
                    checkAlertMessageAndAccept(expectedAlertMessage);

                    return this;
                }
        );
    }

    public DepositPage tryDepositWithoutSelectedAccount(
            String expectedAlertMessage
    ) {
        return StepLogger.ui(
                "Try deposit without selected account",
                () -> {
                    clickDeposit();
                    checkAlertMessageAndAccept(expectedAlertMessage);

                    return this;
                }
        );
    }

    public DepositPage checkAccountBalance(
            double amount,
            String accountNumber
    ) {
        return StepLogger.ui(
                "Check account balance: $" + amount,
                () -> {
                    accountsSelector
                            .shouldHave(text(accountNumber))
                            .shouldHave(text("$" + amount));

                    return this;
                }
        );
    }
}
