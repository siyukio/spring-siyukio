package io.github.siyukio.tools.api.annotation;

import io.github.siyukio.tools.api.token.Token;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorization
 *
 * @author Bugee
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorization {

    State state() default State.REQUIRED;

    /**
     * Principal type.
     *
     * @return
     */
    String type() default Token.PRINCIPAL_TYPE_USER;

    /**
     * Principal scopes.
     *
     * @return
     */
    String[] scopes() default {};

    /**
     * Actor type.
     *
     * @return
     */
    String actorType() default "";

    /**
     * Authorization state enum.
     */
    enum State {
        /**
         * Inherit authorization setting from parent (class-level).
         */
        INHERIT,

        /**
         * Explicitly require authorization.
         */
        REQUIRED,

        /**
         * Explicitly disable authorization (public endpoint).
         */
        DISABLED
    }
}
