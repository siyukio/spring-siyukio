package com.siyukio.tools.api.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

/**
 * Api controller
 *
 * @author Buddy
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@RequestMapping(method = {RequestMethod.POST})
public @interface ApiController {

    /**
     * Alias for {@link Controller#value}.
     */
    @AliasFor(annotation = Controller.class)
    String value() default "";

    /**
     * Alias for {@link RequestMapping#path}.
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] path() default {};

    /**
     * Category tags.
     */
    String[] tags() default {};

    /**
     * Summary
     */
    String summary() default "";

}
