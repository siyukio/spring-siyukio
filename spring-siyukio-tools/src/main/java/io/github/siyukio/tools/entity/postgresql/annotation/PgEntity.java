package io.github.siyukio.tools.entity.postgresql.annotation;

import io.github.siyukio.tools.entity.EntityConstants;

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

    boolean createIndexAuto() default true;

    String dataSource() default EntityConstants.DATASOURCE_NAME;

    String schema() default "";

    String table() default "";

    boolean synchronousCommit() default true;

    String comment();

    PgIndex[] indexes() default {};
}
