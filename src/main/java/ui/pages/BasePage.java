package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Alert;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public abstract class BasePage<T extends BasePage> {
    protected SelenideElement usernameInput = $(Selectors.byAttribute("placeholder", "Username"));
    protected SelenideElement passwordInput = $(Selectors.byAttribute("placeholder","Password"));
    protected SelenideElement accountsSelector = $(".account-selector");
    protected SelenideElement inputAmount = $(Selectors.byAttribute("placeholder","Enter amount"));

    public abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <T extends BasePage> T getPage(Class<T> pageClass){
        return Selenide.page(pageClass);
    }

    public T checkAlertMessageAndAccept(String bankAlert){
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(bankAlert);
        alert.accept();

        return (T) this;
    }
    public T openSelectorAccounts(String accountNumber){
        accountsSelector.selectOptionContainingText(accountNumber);

        return (T)this;
    }

    public T setAmount(double amount){
        inputAmount.setValue(String.valueOf(amount));

        return (T)this;
    }
}
