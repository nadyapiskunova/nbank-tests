package common.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;

public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private Map<String, Long> startTime = new HashMap<>();
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        String testName = extensionContext.getRequiredTestClass().getPackageName() + "."
                + extensionContext.getDisplayName();
        startTime.put(testName, System.currentTimeMillis());
        System.out.println("======== THREAD <<" + Thread.currentThread().getName() + ">>" + " TEST STARTED " + testName +
                " ========");
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        String testName = extensionContext.getRequiredTestClass().getPackageName() + "."
                + extensionContext.getDisplayName();
        long testDuration = System.currentTimeMillis() - startTime.get(testName);
        System.out.println("======== THREAD <<" + Thread.currentThread().getName() + ">>" + " TEST FINISHED " + testName +
                ", TEST DURATION: " + testDuration + " MLS");
    }


}
