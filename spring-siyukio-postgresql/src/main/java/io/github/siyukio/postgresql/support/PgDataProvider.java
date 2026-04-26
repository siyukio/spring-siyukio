package io.github.siyukio.postgresql.support;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.util.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cache manager for managing all Caffeine caches within a single data source.
 * Used for distributed cache invalidation via PostgreSQL LISTEN/NOTIFY.
 *
 * @author Bugee
 */
@Slf4j
public class PgDataProvider {

    private final String suffix = IdUtils.getUniqueId();

    private final Set<String> testSchemaSet = new HashSet<>();

    private final boolean junit = ProfilesUtils.isJUnit();

    private final MultiJdbcTemplate multiJdbcTemplate;
    // Map of schema.table to cache instance
    private final Map<String, Cache<String, JSONObject>> cacheMap = new ConcurrentHashMap<>();
    private volatile Connection listenConnection;

    public PgDataProvider(MultiJdbcTemplate multiJdbcTemplate) {
        this.multiJdbcTemplate = multiJdbcTemplate;
    }

    public String getDbName() {
        return multiJdbcTemplate.getDbName();
    }

    private void ensureConnected() throws SQLException {
        if (listenConnection == null || listenConnection.isClosed()) {
            log.info("Establishing new PostgreSQL LISTEN connection for dbName:{}...", this.multiJdbcTemplate.getDbName());

            listenConnection = multiJdbcTemplate.getMasterDataSource().getConnection();

            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("LISTEN entity_cache_invalidation");
            }

            log.info("LISTEN connection established");
        }
    }

    private void pollNotifications() throws SQLException {
        PGConnection pgConn = listenConnection.unwrap(PGConnection.class);
        PGNotification[] notifications = pgConn.getNotifications(100);

        if (notifications != null) {
            Arrays.stream(notifications).forEach(pgNotification -> {
                try {
                    Notification notification = XDataUtils.parse(
                            pgNotification.getParameter(), Notification.class);
                    Cache<String, JSONObject> cache = cacheMap.get(notification.schema() + "." + notification.table());
                    if (cache != null) {
                        cache.invalidate(notification.id());
                        log.debug("Invalidated cache for {},{},{},{}", notification.operation, notification.schema(), notification.table(), notification.id());
                    }
                } catch (Exception e) {
                    log.error("Error parsing notification: {}", pgNotification.getParameter(), e);
                }
            });
        }
    }

    private void closeConnection() {
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException e) {
                log.error("Error closing PostgreSQL LISTEN connection", e);
            }
        }
    }

    private void dropTestSchemas() {
        testSchemaSet.forEach(schema -> {
            try {
                String sql = PgSqlUtils.dropSchemaSql(schema);
                multiJdbcTemplate.getMaster().execute(sql);
                log.info("Drop PostgreSQL test schema: {}", schema);
            } catch (Exception e) {
                log.error("Error dropping PostgreSQL test schema: {}", schema, e);
            }
        });
    }

    public void destroy() {
        closeConnection();
        dropTestSchemas();
    }

    public void start() {
        AsyncUtils.scheduleWithFixedDelay(() -> {
            try {
                ensureConnected();
                pollNotifications();
            } catch (SQLException e) {
                closeConnection();
                log.error("Error polling PostgreSQL notifications", e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public String registerTestSchema(String schema) {
        if (junit) {
            String testSchema = schema + "_" + suffix;
            if (!testSchemaSet.contains(testSchema)) {
                log.info("Create PostgreSQL test schema: {}", testSchema);
                testSchemaSet.add(testSchema);
            }
            return testSchema;
        }
        return schema;
    }

    /**
     * Register a cache for an entity.
     *
     * @param entityDefinition the entity definition
     */
    public Cache<String, JSONObject> registerCache(EntityDefinition entityDefinition) {
        Cache<String, JSONObject> cache = CacheUtils.createCache(entityDefinition.cacheDefinition());
        cacheMap.put(entityDefinition.schema() + "." + entityDefinition.table(), cache);
        return cache;
    }

    public record Notification(
            String schema,
            String table,
            String id,
            String operation
    ) {
    }

}
