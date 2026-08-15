package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class EditProfilePage extends BasePage<EditProfilePage> {
    private SelenideElement
            inputName =
            $(Selectors.byAttribute("placeholder", "Enter new name")),
            saveChangeBtn = $(".btn.btn-primary.mt-3"),
            userNameLabel = $(".user-name"),
            welcomeUserName = $("h2.welcome-text span");

    @Override
    public String url() {
        return "/edit-profile";
    }

    public EditProfilePage updateName(
            String name,
            String expectedAlertMessage
    ) {
        return StepLogger.ui(
                "Update user name: " + name,
                () -> {
                    setName(name);
                    clickSaveChangeButton();
                    checkAlertMessageAndAccept(expectedAlertMessage);

                    return this;
                }
        );
    }

    public EditProfilePage updateNameWithInvalidData(
            String name,
            String firstExpectedMessage,
            String secondExpectedMessage
    ) {
        return StepLogger.ui(
                "Try to update user name with invalid data: " + name,
                () -> {
                    setName(name);
                    clickSaveChangeButton();
                    checkAlertMessageAndAccept(
                            firstExpectedMessage,
                            secondExpectedMessage
                    );

                    return this;
                }
        );
    }

    public EditProfilePage setName(String name) {
        inputName.sendKeys(name);

        return this;
    }

    public EditProfilePage clickSaveChangeButton() {
        saveChangeBtn.click();

        return this;
    }

    public EditProfilePage checkUserNameLabel(String name) {
        return StepLogger.ui(
                "Check user name: " + name,
                () -> {
                    userNameLabel.shouldHave(text(name));

                    return this;
                }
        );
    }

    public EditProfilePage checkWelcomeUserName(String name) {
        return StepLogger.ui(
                "Check welcome user name: " + name,
                () -> {
                    welcomeUserName.shouldHave(text(name));

                    return this;
                }
        );
    }

    public EditProfilePage openDashboard() {
        Selenide.open("/dashboard");
        return this;
    }

}
