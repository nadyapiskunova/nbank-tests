package common.annotations;

import api.contract.BackendVersion;
import common.extensions.ApiVersionExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.TYPE,
        ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ApiVersionExtension.class)
public @interface APIVersion {
    BackendVersion value();
}