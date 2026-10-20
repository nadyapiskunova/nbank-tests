package common.helpers;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;

import java.io.ByteArrayInputStream;

/*
 Example of usage:

 StepLogger.log("Get all users", () -> {

  click() ->  click log
  post() -> post log

  }

  "Get all users" ->
        "click log"
        "post log"

 */
public class StepLogger {
    @FunctionalInterface
    public interface ThrowableRunnable<T> {
        T run() throws Throwable;
    }

    @FunctionalInterface
    public interface ThrowableVoidRunnable {
        void run() throws Throwable;
    }

    public static <T> T log(String title, ThrowableRunnable<T> runnable) {
        return Allure.step(title, () -> runnable.run());
    }

    public static void log(String title, ThrowableVoidRunnable runnable) {
        Allure.step(title, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T ui(String title, ThrowableRunnable<T> runnable) {
        return Allure.step(title, () -> {
            T result = runnable.run();

            attachScreenshot();

            return result;
        });
    }

    public static void ui(String title, ThrowableVoidRunnable runnable) {
        Allure.step(title, () -> {
            runnable.run();

            attachScreenshot();

            return null;
        });
    }

    private static void attachScreenshot() {
        byte[] screenshot = Selenide.screenshot(OutputType.BYTES);

        Allure.attachment(
                "Screenshot",
                new ByteArrayInputStream(screenshot)
        );
    }
}
