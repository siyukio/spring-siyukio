package io.github.siyukio.tools.cache.definition;

import java.util.concurrent.TimeUnit;

/**
 * @author Bugee
 */
public record CacheDefinition(
        long maximumSize,
        boolean softValues,
        TimeUnit expireUnit,
        long expireAfterAccess,
        long expireAfterWrite
) {
}
