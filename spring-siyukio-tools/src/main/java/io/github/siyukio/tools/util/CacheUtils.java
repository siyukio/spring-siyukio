package io.github.siyukio.tools.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.github.siyukio.tools.cache.definition.CacheDefinition;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Bugee
 */
public abstract class CacheUtils {

    private final static Scheduler EXECUTOR_SERVICE_SCHEDULER = Scheduler.forScheduledExecutorService(AsyncUtils.SINGLE_EXECUTOR_SERVICE);

    public static <T> Cache<String, T> createCache(CacheDefinition cacheDefinition) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .scheduler(EXECUTOR_SERVICE_SCHEDULER);
        builder.maximumSize(cacheDefinition.maximumSize());
        if (cacheDefinition.softValues()) {
            builder.softValues();
        }
        TimeUnit expireUnit = cacheDefinition.expireUnit();
        if (cacheDefinition.expireAfterAccess() > 0) {
            builder.expireAfterAccess(cacheDefinition.expireAfterAccess(), expireUnit);
        }
        if (cacheDefinition.expireAfterWrite() > 0) {
            builder.expireAfterWrite(cacheDefinition.expireAfterWrite(), expireUnit);
        }
        return builder.build();
    }

    public static Cache<String, String> createCache(long maximumSize) {
        return Caffeine.newBuilder()
                .scheduler(EXECUTOR_SERVICE_SCHEDULER)
                .maximumSize(maximumSize)
                .softValues()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
}
