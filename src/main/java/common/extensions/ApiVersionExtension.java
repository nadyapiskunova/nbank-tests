package common.extensions;

import api.configs.Config;
import api.contract.BackendVersion;
import common.annotations.APIVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

public class ApiVersionExtension implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

        Optional<APIVersion> annotation = context.getElement()
                .map(element -> element.getAnnotation(APIVersion.class));

        if (annotation.isEmpty()) {
            return ConditionEvaluationResult.enabled(
                    "APIVersion is not specified"
            );
        }

        BackendVersion requiredVersion = annotation.get().value();
        BackendVersion currentVersion = Config.getBackendVersion();

        if (requiredVersion == currentVersion) {
            return ConditionEvaluationResult.enabled(
                    "API version matches: " + currentVersion
            );
        }

        return ConditionEvaluationResult.disabled(
                "Test is for " + requiredVersion
                        + ", current backend is " + currentVersion
        );
    }
}