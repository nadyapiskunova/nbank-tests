package iteration_1.ui;

import api.configs.Config;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import iteration_1.api.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public class BaseUITest extends BaseTest {
    public void authAsUser(String username, String password) {
        Selenide.open("/");
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
    }

    @BeforeAll
    public static void setupSelenoid(){
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }
}
