package iteration_1.ui;

import api.configs.Config;
import com.codeborne.selenide.Configuration;
import common.extensions.AdminSessionExtension;
import common.extensions.UiUserSessionExtension;
import common.extensions.UserSessionExtension;
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
    public static void setupSelenoid(){
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");
        Configuration.headless = true;

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @AfterEach
    public void closeBrowser() {
        closeWebDriver();
    }
}
