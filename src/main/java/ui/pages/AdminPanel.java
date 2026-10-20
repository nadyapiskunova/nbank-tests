package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;
import ui.elements.UserBage;

import java.util.List;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class AdminPanel extends BasePage<AdminPanel> {
    private SelenideElement adminPanelText = $(Selectors.byText("Admin Panel"));
    private SelenideElement addUserButton = $(Selectors.byText("Add User"));
    private SelenideElement usersList = $(Selectors.byText("All Users")).parent();
    private ElementsCollection users = $(Selectors.byText("All Users")).parent().findAll("li");

    @Override
    public String url() {
        return "/admin";
    }

    public AdminPanel createUser(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        addUserButton.click();

        return this;
    }

    public AdminPanel createUserAndAcceptAlert(
            String username,
            String password,
            String expectedMessage
    ) {
        return StepLogger.ui(
                "Create user: " + username,
                () -> {
                    usernameInput.sendKeys(username);
                    passwordInput.sendKeys(password);
                    addUserButton.click();

                    checkAlertMessageAndAccept(expectedMessage);

                    return this;
                }
        );
    }

    public List<UserBage> getAllUsers() {
        return StepLogger.log(
                "Get all users from Dashboard",
                () -> generatePageElement(users, UserBage::new)
        );
    }

    public UserBage findUserByUsername(String username) {
        return StepLogger.log(
                "Find user by username: " + username,
                () -> new UserBage(
                        users
                                .findBy(text(username))
                                .shouldBe(visible)
                )
        );
    }
}
