package io.github.siyukio.postgresql.registrar;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.siyukio.postgresql.support.*;
import io.github.siyukio.tools.datasource.MultiDataSourceProperties;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.registrar.AbstractCommonRegistrar;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bugee
 */
@Slf4j
public class PostgresqlEntityRegistrar extends AbstractCommonRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private final static Map<String, MultiJdbcTemplate> JDBC_TEMPLATE_MAP = new HashMap<>();
    private final static ConcurrentHashMap<String, PgDataProvider> PG_CACHE_MANAGER_MAP = new ConcurrentHashMap<>();
    private final static PgDataManager PG_DATA_MANAGER = new PgDataManager();

    public static MultiJdbcTemplate getMultiJdbcTemplate(String dbName) {
        MultiJdbcTemplate multiJdbcTemplate = JDBC_TEMPLATE_MAP.get(dbName);
        Assert.state(multiJdbcTemplate != null, "No postgres db set: " + dbName);
        return multiJdbcTemplate;
    }

    public static PgDataProvider getPgDataProvider(String dbName) {
        return PG_CACHE_MANAGER_MAP.computeIfAbsent(dbName, k -> {
            MultiJdbcTemplate multiJdbcTemplate = getMultiJdbcTemplate(dbName);
            PgDataProvider pgDataProvider = new PgDataProvider(multiJdbcTemplate);
            pgDataProvider.start();
            PG_DATA_MANAGER.register(dbName, pgDataProvider);
            return pgDataProvider;
        });
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

    private String registerMultiJdbcTemplate(BeanDefinitionRegistry registry, MultiJdbcTemplate multiJdbcTemplate) {
        // Register dataSource
        String dataSourceName = "dataSource";
        boolean primary = true;
        if (StringUtils.hasText(multiJdbcTemplate.getDbName())) {
            dataSourceName = multiJdbcTemplate.getDbName() + "DataSource";
            primary = false;
        }
        BeanDefinition dataSourceBeanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(DataSource.class, multiJdbcTemplate::getMasterDataSource)
                .setPrimary(primary)
                .getBeanDefinition();
        registry.registerBeanDefinition(dataSourceName, dataSourceBeanDefinition);
        log.info("Bootstrapping register dataSource: {}, {}", dataSourceName, dataSourceBeanDefinition);
        return dataSourceName;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
                                        BeanNameGenerator generator) {
        super.registerBeanDefinitions(metadata, registry, generator);

        Set<String> dataSourceNames = new HashSet<>();
        //bind spring.datasource.postgres
        MultiDataSourceProperties multiDataSourceProperties = XDataUtils.safeBind(MultiDataSourceProperties.CONFIG_PREFIX, Bindable.of(MultiDataSourceProperties.class), this.environment);
        if (multiDataSourceProperties != null) {
            MultiJdbcTemplate multiJdbcTemplate = this.createMultiJdbcTemplate("", multiDataSourceProperties);
            String dataSourceName = this.registerMultiJdbcTemplate(registry, multiJdbcTemplate);
            dataSourceNames.add(dataSourceName);
        } else {
            //bind spring.datasource.postgres-multi
            Map<String, MultiDataSourceProperties> groupMap = XDataUtils.safeBind(
                    MultiDataSourceProperties.MULTI_CONFIG_PREFIX,
                    Bindable.mapOf(String.class, MultiDataSourceProperties.class), this.environment);
            if (groupMap != null) {
                groupMap.forEach((key, value) -> {
                    MultiJdbcTemplate multiJdbcTemplate = this.createMultiJdbcTemplate(key, value);
                    String dataSourceName = this.registerMultiJdbcTemplate(registry, multiJdbcTemplate);
                    dataSourceNames.add(dataSourceName);
                });
            }
        }

        // Register PgDataProvider
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                .genericBeanDefinition(PgDataManager.class, () -> PG_DATA_MANAGER)
                .setPrimary(true);
        dataSourceNames.forEach(beanDefinitionBuilder::addDependsOn);
        BeanDefinition dataProviderBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
        registry.registerBeanDefinition("pgDataManager", dataProviderBeanDefinition);
        log.info("Bootstrapping register PgDataManager: {}", dataProviderBeanDefinition);
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

    private MultiJdbcTemplate createMultiJdbcTemplate(String dbName, MultiDataSourceProperties dbProps) {
        HikariDataSource masterDataSource = this.buildDataSource(dbName, dbProps.getMaster(), dbProps.getHikari(), "master");
        List<DataSource> slaveDataSources = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dbProps.getSlaves())) {
            for (MultiDataSourceProperties.DbNode slave : dbProps.getSlaves()) {
                slaveDataSources.add(this.buildDataSource(dbName, slave, dbProps.getHikari(), "slave"));
            }
        }
        MultiJdbcTemplate multiJdbcTemplate = new MultiJdbcTemplate(dbName, masterDataSource, slaveDataSources, dbProps.getMasterKey());
        JDBC_TEMPLATE_MAP.put(dbName, multiJdbcTemplate);

        // Create cache invalidation function
        JdbcTemplate jdbcTemplate = multiJdbcTemplate.getMaster();
        jdbcTemplate.execute(PgSqlUtils.CREATE_CACHE_INVALIDATION_FUNCTION_SQL);

        return multiJdbcTemplate;
    }
}
