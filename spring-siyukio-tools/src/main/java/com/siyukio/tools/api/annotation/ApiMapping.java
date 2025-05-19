package com.siyukio.tools.api.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.annotation.*;

/**
 * Api mapping.
 *
 * @author Buddy
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ResponseBody
@CrossOrigin
@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET})
public @interface ApiMapping {

    /**
     * Alias for {@link RequestMapping#path}.
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] path();

    /**
     * Whether the API requires authorization validation.
     */
    boolean authorization() default true;

    /**
     * Whether the API requires parameter signature validation.
     */
    boolean signature() default false;

    /**
     * Validates whether the authorization contains the specified role.
     */
    String[] roles() default "";

    /**
     * Summary.
     */
    String summary() default "";

    /**
     * Description.
     */
    String description() default "";

    /**
     * Whether the API is deprecated.
     */
    boolean deprecated() default false;
}
