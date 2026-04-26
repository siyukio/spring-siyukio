package io.github.siyukio.postgresql.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Bugee
 */
@Slf4j
public class PgCacheManager implements DisposableBean {

    private final Map<String, PgCacheProvider> pgCacheProviderMap = new ConcurrentHashMap<>();

    public void register(String dbName, PgCacheProvider pgCacheProvider) {
        pgCacheProviderMap.put(dbName, pgCacheProvider);
    }

    @Override
    public void destroy() throws Exception {
        log.info("Destroy PgCacheManager");
        pgCacheProviderMap.forEach((key, pgCacheProvider) -> {
            log.info("Close PgCacheProvider: {}", pgCacheProvider.getDbName());
            pgCacheProvider.closeConnection();
        });
    }
}
