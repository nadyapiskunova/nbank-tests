package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

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

    public EditProfilePage setName(String name){
        inputName.sendKeys(name);

        return this;
    }

    public EditProfilePage clickSaveChangeButton(){
        saveChangeBtn.click();

        return this;
    }

    public EditProfilePage checkUserNameLabel(String name){
        userNameLabel.shouldHave(text(name));

        return this;
    }

    public EditProfilePage checkWelcomeUserName(String name){
        welcomeUserName.shouldHave(text(name));

        return this;
    }
    public EditProfilePage openDashboard() {
        Selenide.open("/dashboard");
        return this;
    }

}
