package common.extensions;

import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import storage.SessionStorage;

public class UserSessionExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        UserSession annotation = extensionContext
                .getRequiredTestMethod()
                .getAnnotation(UserSession.class);

        if (annotation == null) {
            return;
        }

        SessionStorage.clear();

        int userCount = annotation.value();

        for (int i = 0; i < userCount; i++) {
            AdminSteps.createUser();
        }
    }
}