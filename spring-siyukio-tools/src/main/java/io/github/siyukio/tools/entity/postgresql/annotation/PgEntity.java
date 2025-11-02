package io.github.siyukio.tools.entity.postgresql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bugee
 */

@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PgEntity {

    boolean createTableAuto() default true;

    boolean addColumnAuto() default true;

    boolean createIndexAuto() default true;

    String dbName() default "";

    String schema() default "";

    String table() default "";

    String comment();

    PgIndex[] indexes() default {};
}
