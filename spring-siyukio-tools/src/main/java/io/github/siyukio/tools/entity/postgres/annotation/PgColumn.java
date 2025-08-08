package io.github.siyukio.tools.entity.postgres.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bugee
 */

@Target(value = {ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PgColumn {

    String name() default "";

    int length() default 255;

    String defaultValue() default "";

    String comment();
}
