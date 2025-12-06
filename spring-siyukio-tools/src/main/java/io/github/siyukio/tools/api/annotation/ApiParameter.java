package io.github.siyukio.tools.api.annotation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Api request parameters.
 *
 * @author Buddy
 */
@Target({TYPE, RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonIgnore
public @interface ApiParameter {

    /**
     * Whether it is required, default is true.
     */
    boolean required() default true;

    /**
     * For optional parameters, if the request does not provide this parameter, the default value will be used.
     */
    String defaultValue() default "";

    /**
     * Description.
     */
    String description() default "";

    /**
     * If the parameter is a number, sets the maximum allowed value for the parameter.
     */
    long maximum() default Long.MAX_VALUE;

    /**
     * If the parameter is a number, sets the minimum allowed value for the parameter.
     */
    long minimum() default Long.MIN_VALUE;

    /**
     * If the parameter is a String, sets the maximum length of the String.
     * Default is 1024.
     */
    int maxLength() default 1024;

    /**
     * If the parameter is a String, validates the text content using a regular expression.
     */
    String pattern() default "";

    /**
     * If the parameter is a String, sets the minimum length of the String.
     * Default is 0.
     */
    int minLength() default 0;

    /**
     * If the parameter is an array, sets the maximum number of elements.
     * Default is 1024.
     */
    int maxItems() default 1024;

    /**
     * If the parameter is an array, sets the minimum number of elements.
     * Default is 0.
     */
    int minItems() default 0;

    /**
     * Validation failure message.
     */
    String error() default "";

    /**
     * Whether the current parameter is a password.
     */
    boolean password() default false;

    /**
     * Parameter example.
     */
    Example[] examples() default {};


}
