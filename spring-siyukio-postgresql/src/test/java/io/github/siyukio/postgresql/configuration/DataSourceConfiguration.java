package io.github.siyukio.postgresql.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * @author Bugee
 */
@Configuration
public class DataSourceConfiguration {

    @Autowired
    private Environment env;


    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("POSTGRESQL_URL"));
        config.setUsername(env.getProperty("POSTGRESQL_USERNAME"));
        config.setPassword(env.getProperty("POSTGRESQL_PASSWORD"));

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.setConnectionTimeout(12000);
        config.setValidationTimeout(6000);

        config.setConnectionTestQuery("SELECT 1");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }
}
