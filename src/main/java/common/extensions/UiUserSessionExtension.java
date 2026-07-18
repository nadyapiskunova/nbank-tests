package common.extensions;

import common.annotations.UserSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import storage.SessionStorage;
import ui.pages.BasePage;

public class UiUserSessionExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        UserSession annotation = extensionContext
                .getRequiredTestMethod()
                .getAnnotation(UserSession.class);
        if (annotation == null) {
            return;
        }
        int userNumberForAuthorization = annotation.auth();
        BasePage.authAsUser(
                SessionStorage.getUser(userNumberForAuthorization)
        );
    }
}