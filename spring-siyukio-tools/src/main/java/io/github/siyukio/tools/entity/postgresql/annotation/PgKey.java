package io.github.siyukio.tools.entity.postgresql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bugee
 */

@Target(value = {ElementType.RECORD_COMPONENT})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PgKey {

    String column() default "";

    boolean generated() default true;

    String comment() default "";
}
