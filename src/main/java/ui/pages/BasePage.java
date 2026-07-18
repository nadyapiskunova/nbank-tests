package ui.pages;

import api.models.CreateUserRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Alert;
import ui.elements.BaseElement;

import java.util.List;
import java.util.function.Function;

import static com.codeborne.selenide.Selenide.*;
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
    public T checkAlertMessageAndAccept(String... expectedMessages) {
        Alert alert = switchTo().alert();
        String actualMessage = alert.getText();

        assertThat(expectedMessages)
                .anySatisfy(expectedMessage ->
                        assertThat(actualMessage).contains(expectedMessage)
                );
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

    public static void authAsUser(String username, String password) {
        Selenide.open("/");
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
    }

    public static void authAsUser(CreateUserRequest createUserRequest){
        authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword());
    }


    protected <T extends BaseElement> List<T> generatePageElement(ElementsCollection elementsCollection, Function<SelenideElement, T> constructor){
        return elementsCollection.stream().map(constructor).toList();
    }
}
