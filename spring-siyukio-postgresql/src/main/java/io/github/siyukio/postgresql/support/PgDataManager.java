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
public class PgDataManager implements DisposableBean {

    private final Map<String, PgDataProvider> pgCacheProviderMap = new ConcurrentHashMap<>();

    public void register(String dbName, PgDataProvider pgDataProvider) {
        pgCacheProviderMap.put(dbName, pgDataProvider);
    }

    @Override
    public void destroy() throws Exception {
        log.info("Destroy PgDataManager");
        pgCacheProviderMap.forEach((key, pgDataProvider) -> {
            log.info("Close PgDataProvider: {}", pgDataProvider.getDbName());
            pgDataProvider.destroy();
        });
    }
}
