package io.github.siyukio.postgresql.registrar;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.siyukio.postgresql.support.MultiJdbcTemplate;
import io.github.siyukio.postgresql.support.PgEntityFactoryBean;
import io.github.siyukio.tools.datasource.MultiDataSourceProperties;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.registrar.AbstractCommonRegistrar;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bugee
 */
@Slf4j
public class PostgresqlEntityRegistrar extends AbstractCommonRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private final static Map<String, MultiJdbcTemplate> JDBC_TEMPLATE_MAP = new HashMap<>();

    public static MultiJdbcTemplate getMultiJdbcTemplate(String dbName) {
        MultiJdbcTemplate multiJdbcTemplate = JDBC_TEMPLATE_MAP.get(dbName);
        Assert.state(multiJdbcTemplate != null, "No postgres db set: " + dbName);
        return multiJdbcTemplate;
    }

    @Override
    protected String getTopic() {
        return "siyukio-postgres-entity";
    }

    @Override
    protected Class<? extends Annotation> getAnnotationType() {
        return PgEntity.class;
    }

    @Override
    protected Class<?> getBeanFactoryClass() {
        return PgEntityFactoryBean.class;
    }

    @Override
    protected Class<?> getBeanClass() {
        return PgEntityDao.class;
    }

    @Override
    protected HikariDataSource createDataSource() {
        HikariDataSource masterDataSource = null;
        //bind spring.datasource.postgres
        MultiDataSourceProperties multiDataSourceProperties = XDataUtils.safeBind(MultiDataSourceProperties.CONFIG_PREFIX, Bindable.of(MultiDataSourceProperties.class), this.environment);
        if (multiDataSourceProperties != null) {
            masterDataSource = this.createMultiJdbcTemplate("", multiDataSourceProperties);
        } else {
            //bind spring.datasource.postgres-multi
            Map<String, MultiDataSourceProperties> groupMap = XDataUtils.safeBind(
                    "spring.datasource.postgres-multi",
                    Bindable.mapOf(String.class, MultiDataSourceProperties.class), this.environment);
            if (groupMap != null) {
                for (Map.Entry<String, MultiDataSourceProperties> entry : groupMap.entrySet()) {
                    if (masterDataSource == null) {
                        masterDataSource = this.createMultiJdbcTemplate(entry.getKey(), entry.getValue());
                    } else {
                        this.createMultiJdbcTemplate(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        if (masterDataSource == null) {
            throw new IllegalStateException("No postgres dataSource config set");
        }
        return masterDataSource;
    }

    private HikariDataSource buildDataSource(String dbName, MultiDataSourceProperties.DbNode node, HikariConfig baseConfig, String nodeType) {
        HikariConfig config = new HikariConfig();
        if (baseConfig != null) {
            baseConfig.copyStateTo(config);
        }
        config.setJdbcUrl(node.getUrl());
        config.setUsername(node.getUsername());
        config.setPassword(node.getPassword());
        if (!StringUtils.hasText(dbName)) {
            dbName = "default";
        }
        config.setPoolName("postgres-" + dbName + "-" + nodeType);
        return new HikariDataSource(config);
    }

    private HikariDataSource createMultiJdbcTemplate(String dbName, MultiDataSourceProperties dbProps) {
        HikariDataSource masterDataSource = this.buildDataSource(dbName, dbProps.getMaster(), dbProps.getHikari(), "master");
        List<DataSource> slaveDataSources = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dbProps.getSlaves())) {
            for (MultiDataSourceProperties.DbNode slave : dbProps.getSlaves()) {
                slaveDataSources.add(this.buildDataSource(dbName, slave, dbProps.getHikari(), "slave"));
            }
        }
        MultiJdbcTemplate multiJdbcTemplate = new MultiJdbcTemplate(masterDataSource, slaveDataSources);
        JDBC_TEMPLATE_MAP.put(dbName, multiJdbcTemplate);
        return masterDataSource;
    }
}
