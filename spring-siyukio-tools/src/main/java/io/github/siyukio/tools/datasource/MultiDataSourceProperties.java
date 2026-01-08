package io.github.siyukio.tools.datasource;

import com.zaxxer.hikari.HikariConfig;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-data source configuration properties for PostgreSQL.
 * <p>
 * Provides master-slave database configuration with HikariCP connection pooling.
 *
 * @author Bugee
 */
public class MultiDataSourceProperties {

    /**
     * Configuration prefix for single data source instance.
     */
    public static final String CONFIG_PREFIX = "spring.datasource.postgres";

    /**
     * Configuration prefix for multiple data source instances.
     * <p>
     * Used when configuring multiple named data source instances.
     */
    public static final String MULTI_CONFIG_PREFIX = "spring.datasource.postgres-multi";

    /**
     * HikariCP connection pool configuration.
     */
    private HikariConfig hikari;

    /**
     * Master database node configuration.
     * <p>
     * Used for write operations and primary read operations.
     */
    private DbNode master;

    /**
     * Slave database nodes configuration.
     * <p>
     * Used for read operations and load balancing. Can be empty for single-database setup.
     */
    private List<DbNode> slaves = new ArrayList<>();

    public HikariConfig getHikari() {
        return hikari;
    }

    public void setHikari(HikariConfig hikari) {
        Assert.notNull(hikari, "Hikari configuration must not be null");
        this.hikari = hikari;
    }

    public DbNode getMaster() {
        return master;
    }

    public void setMaster(DbNode master) {
        Assert.notNull(master, "Master database node must not be null");
        this.master = master;
    }

    public List<DbNode> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<DbNode> slaves) {
        Assert.notNull(slaves, "Slave database nodes must not be null");
        this.slaves = slaves;
    }

    /**
     * Database node configuration properties.
     * <p>
     * Represents a single database connection with connection parameters.
     */
    public static class DbNode {

        /**
         * JDBC connection URL for the database.
         * <p>
         * Example: jdbc:postgresql://localhost:5432/mydb
         */
        private String url;

        /**
         * Database username for authentication.
         */
        private String username;

        /**
         * Database password for authentication.
         */
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            Assert.hasText(url, "Database URL must not be empty");
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            Assert.hasText(username, "Database username must not be empty");
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            Assert.notNull(password, "Database password must not be null");
            this.password = password;
        }
    }
}