package iteration_1.ui;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CreateAccountTest extends BaseUITest {
    @Test
    public void userCanCreateAccountTest(){
        // шаги по настройке окружения
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке

        CreateUserRequest user = AdminSteps.createUser(createdUserIds);

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK(), Endpoint.LOGIN)
                .post(
                        LoginUserRequest.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/dashboard");

        // шаг теста
        // ШАГ 4: юзер создает аккаунт
        $(Selectors.byText("➕ Create New Account")).click();

        // ШАГ 5: аккаунт был создан на UI
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alert.getText()).contains("✅ New Account Created! Account Number:");
        alert.accept();

        Pattern pattern = Pattern.compile("Account Number: (\\w+)");
        Matcher matcher = pattern.matcher(alertText);
        matcher.find();

        String createdAccNumber = matcher.group(1);

        // ШАГ 6: аккаунт был создан на API
        AccountResponse[] existingUserAccounts = given()
                .spec(RequestSpecs.authAsUser(user.getUsername(), user.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(AccountResponse[].class);

        assertThat(existingUserAccounts).hasSize(1);

        AccountResponse createdAccount = existingUserAccounts[0];

        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getBalance()).isZero();
    }
}
