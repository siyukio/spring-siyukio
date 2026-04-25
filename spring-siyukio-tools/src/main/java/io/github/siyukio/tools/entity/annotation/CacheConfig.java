package io.github.siyukio.tools.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for configuring Caffeine cache settings.
 *
 * @author Bugee
 */
@Target(value = {ElementType.ANNOTATION_TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface CacheConfig {

    /**
     * Maximum number of entries the cache may contain.
     * <p>
     * Set to 0 to disable caching.
     *
     * @return the maximum size, defaults to 0 (disabled)
     */
    long maximumSize() default 0;

    /**
     * Whether to use soft values for entries.
     * <p>
     * Soft values are garbage-collected when memory is low.
     * Suitable for caching where memory pressure is a concern.
     *
     * @return true if soft values should be used, defaults to false
     */
    boolean softValues() default false;

    /**
     * Time unit for expiration duration.
     *
     * @return the time unit, defaults to MINUTES
     */
    TimeUnit expireUnit() default TimeUnit.MINUTES;

    /**
     * Duration after which entries will expire after last access.
     * <p>
     * Set to 0 to disable expiration based on access time.
     *
     * @return the expiration duration, defaults to 60
     */
    long expireAfterAccess() default 60;

    /**
     * Duration after which entries will expire after write.
     *
     * @return the expiration duration, defaults to 15 minutes
     */
    long expireAfterWrite() default 15;
}
