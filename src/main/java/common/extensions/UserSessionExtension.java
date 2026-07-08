package common.extensions;

import api.models.CreateUserRequest;
import common.annotations.UserSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import storage.SessionStorage;
import ui.pages.BasePage;
import ui.steps.UiAdminSteps;

import java.util.LinkedList;
import java.util.List;

public class UserSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        //ШАГ 1: проверка, что у теста есть аннотация, которая называется UserSession
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if(annotation != null) {
            int userCount = annotation.value();

            SessionStorage.clear();

            List<CreateUserRequest> users = new LinkedList<>();

            for(int i = 0; i < userCount; i++) {
                CreateUserRequest user = UiAdminSteps.createUser();
                users.add(user);
            }

            SessionStorage.addUsers(users);
            int authAsUser = annotation.auth();
            BasePage.authAsUser(SessionStorage.getUser(authAsUser));
        }
    }
}
