package common.extensions;

import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import storage.SessionStorage;
import ui.pages.BasePage;

public class UserSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        //ШАГ 1: проверка, что у теста есть аннотация, которая называется UserSession
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if(annotation != null) {
            int userCount = annotation.value();
            SessionStorage.clear();
            for(int i = 0; i < userCount; i++) {
                AdminSteps.createUser();
            }
            int authAsUser = annotation.auth();
            BasePage.authAsUser(SessionStorage.getUser(authAsUser));
        }
    }
}
