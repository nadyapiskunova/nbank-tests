package iteration_2.ui;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.CustomerResponse;
import api.models.LoginUserRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UpdateNameUserTest extends BaseTest{

    @Disabled("Баг: имя не обновляется без рефреша страницы в .user-name")
    @Test
    public void userCanUpdateNameWithValidDataTest(){
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
        Selenide.open("/edit-profile");

        String name = RandomData.getValidName();
        $(Selectors.byAttribute("placeholder", "Enter new name"))
                .sendKeys(name);;

        $(".btn.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("✅ Name updated successfully!");
        alert.accept();

       $(".user-name").shouldHave(text(name));

        Selenide.open("/dashboard");
        $("h2.welcome-text span").shouldHave(text(name));

        CustomerResponse updatedName =
                new ValidatedCrudRequester<CustomerResponse>(
                        RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                        ResponseSpecs.requestReturnsOK(),
                        Endpoint.CUSTOMER_PROFILE)
                        .get();

        softly.assertThat(updatedName.getName()).isEqualTo(name);
    }

    @Disabled("Баг: имя не обновляется без рефреша страницы в .user-name")
    @Test
    public void userCannotUpdateNameWithInvalidDataTest(){
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
        Selenide.open("/edit-profile");

        String name = RandomData.getNameWithoutSurname();
        $(Selectors.byAttribute("placeholder", "Enter new name"))
                .sendKeys(name);;

        $(".btn.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Name must contain two words with letters only");
        alert.accept();
    }
}
