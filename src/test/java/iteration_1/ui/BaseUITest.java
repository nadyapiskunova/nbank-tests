package iteration_1.ui;

import api.configs.Config;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.extensions.AdminSessionExtension;
import common.extensions.UiUserSessionExtension;
import common.extensions.UserSessionExtension;
import io.qameta.allure.selenide.AllureSelenide;
import iteration_1.api.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static com.codeborne.selenide.Selenide.closeWebDriver;

@ExtendWith({
        AdminSessionExtension.class,
        UserSessionExtension.class,
        UiUserSessionExtension.class
})
public class BaseUITest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("selenoid.url") + "/wd/hub";
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");
        Configuration.headless = true;
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        SelenideLogger.addListener("AllureSelenide",
                new AllureSelenide().screenshots(true).savePageSource(true)
        );

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @AfterEach
    public void closeBrowser() {
        closeWebDriver();
    }
}
