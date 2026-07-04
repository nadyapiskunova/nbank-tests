package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class DepositPage extends BasePage<DepositPage> {
    private SelenideElement depositButton =
            $(Selectors.byText("💵 Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage clickDeposit(){
        depositButton.click();
        return this;
    }

    public DepositPage makeDeposit(double amount){
        setAmount(amount);
        clickDeposit();

        return this;
    }

    public DepositPage checkAccountBalance(double amount, String accountNumber){
        accountsSelector.shouldHave(text(accountNumber))
                .shouldHave(text("$" + amount));
        return this;
    }



}
