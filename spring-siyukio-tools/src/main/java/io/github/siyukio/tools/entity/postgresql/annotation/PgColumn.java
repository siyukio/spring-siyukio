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
public @interface PgColumn {

    String column() default "";

    String defaultValue() default "";

    String comment() default "";

    /**
     * Flag to indicate whether the column value should be encrypted when stored.
     * <p>
     * When true, the column value will be encrypted before persisting to database
     * and decrypted when reading from database. Default is false.
     *
     * @return true if encryption is enabled, false otherwise
     */
    boolean encrypted() default false;
}
