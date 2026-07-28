package common.helpers;

import api.configs.Config;

public final class DbCheck {

    private DbCheck() {
    }

    public static void run(Runnable check) {
        if (Config.getBackendVersion().isDatabaseSupported()) {
            check.run();
        }
    }
}