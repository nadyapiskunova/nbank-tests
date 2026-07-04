package iteration_2.ui;

import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.constans.TestConstants;
import api.generators.RandomData;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AccountsDepositTest extends BaseTest{
    @Test
    public void userCanDepositWithValidData(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse createdAccount = UserSteps.createAccount(user);

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/deposit");

        $(".account-selector").selectOptionContainingText(createdAccount.getAccountNumber());

        Double amount = RandomData.getValidDepositAmount();
        $(Selectors.byAttribute("placeholder","Enter amount"))
                .setValue(String.valueOf(amount));
        $(Selectors.byText("💵 Deposit")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("✅ Successfully deposited");
        alert.accept();

        Selenide.open("/deposit");
        $(".account-selector").selectOptionContainingText(createdAccount.getAccountNumber());

        $("select.account-selector")
                .shouldHave(text(createdAccount.getAccountNumber()))
                .shouldHave(text("$" + amount));

        AccountResponse depositedAccount = UserSteps.getAccounts(user).get(0);
        softly.assertThat(depositedAccount.getBalance()).isEqualTo(amount);
    }

    @Test
    public void userCannotDepositWithInvalidData(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse createdAccount = UserSteps.createAccount(user);

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(), Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/deposit");

        $(".account-selector").selectOptionContainingText(createdAccount.getAccountNumber());

        Double amount = TestConstants.ZERO_AMOUNT;
        $(Selectors.byAttribute("placeholder","Enter amount"))
                .setValue(String.valueOf(amount));
        $(Selectors.byText("💵 Deposit")).click();

        Alert alert = switchTo().alert();

        assertThat(alert.getText()).contains("❌ Please enter a valid amount.");
        alert.accept();
    }

    @Test
    public void userCannotDepositWithoutSelectedAccountTest(){
        CreateUserRequest user = AdminSteps.createUser(createdUserIds);
        AccountResponse createdAccount = UserSteps.createAccount(user);

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(), Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/deposit");

        $(Selectors.byText("💵 Deposit")).click();

        Alert alert = switchTo().alert();

        assertThat(alert.getText()).contains("❌ Please select an account.");
        alert.accept();
    }
}




