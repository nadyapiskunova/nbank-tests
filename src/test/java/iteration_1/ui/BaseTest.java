package iteration_1.ui;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseTest {
    protected List<Integer> createdUserIds = new ArrayList<>();
    @BeforeAll
    public static void setupSelenoid(){
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.3.44:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }
    @AfterEach
    public void deleteUsers() {
        for (Integer userId : createdUserIds) {
            new CrudRequester(
                    RequestSpecs.adminSpec(),
                    ResponseSpecs.requestReturnsOK(), Endpoint.ADMIN_USER)
                    .delete(userId);
        }
        createdUserIds.clear();
    }
}
