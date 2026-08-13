package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;
import org.openqa.selenium.Alert;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAcc = $(Selectors.byText("➕ Create New Account"));

    @Override
    public String url() {
        return "/dashboard";
    }

    public String createUserAccountAndAcceptAlert() {
        return StepLogger.ui(
                "Create new account",
                () -> {
                    createNewAcc.click();

                    Alert alert = switchTo().alert();
                    String alertText = alert.getText();
                    alert.accept();

                    return alertText;
                }
        );
    }
}
