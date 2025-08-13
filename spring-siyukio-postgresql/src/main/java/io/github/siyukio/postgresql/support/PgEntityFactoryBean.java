package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityConstants;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.definition.IndexDefinition;
import io.github.siyukio.tools.entity.definition.KeyDefinition;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgIndex;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;
import io.github.siyukio.tools.util.DateUtils;
import io.github.siyukio.tools.util.EntityUtils;
import io.github.siyukio.tools.util.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Bugee
 */
@Slf4j
public class PgEntityFactoryBean implements FactoryBean<PgEntityDao<?>>, InitializingBean, ApplicationContextAware {

    private final static Map<String, JdbcTemplate> JDBC_TEMPLATE_MAP = new HashMap<>();

    private final Class<?> entityClass;

    @Getter
    @Setter
    private boolean lazyInit = false;

    private Lazy<PgEntityDao<?>> repository;

    private ApplicationContext applicationContext;

    public PgEntityFactoryBean(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public PgEntityDao<?> getObject() throws Exception {
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return PgEntityDao.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.repository = Lazy.of(this::newInstance);
        if (!this.lazyInit) {
            this.repository.get();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private JdbcTemplate getJdbcTemplate(String dataSourceName) {
        JdbcTemplate jdbcTemplate = JDBC_TEMPLATE_MAP.get(dataSourceName);
        if (jdbcTemplate == null) {
            Map<String, DataSource> dataSourceMap = this.applicationContext.getBeansOfType(DataSource.class);
            DataSource dataSource = dataSourceMap.get(dataSourceName);
            Assert.notNull(dataSource, String.format(EntityConstants.ERROR_DATASOURCE_IS_NULL_FORMAT, dataSourceName));

            jdbcTemplate = new JdbcTemplate(dataSource);
            JDBC_TEMPLATE_MAP.put(dataSourceName, jdbcTemplate);
        }
        return jdbcTemplate;
    }

    private ColumnDefinition getColumnDefinition(Field field) {
        PgColumn pgColumn = field.getAnnotation(PgColumn.class);
        assert pgColumn != null;
        ColumnType columnType = EntityUtils.getColumnType(field);
        String fieldName = field.getName();
        String columnName;
        if (StringUtils.hasText(pgColumn.column())) {
            columnName = pgColumn.column();
        } else {
            columnName = EntityUtils.camelToSnake(fieldName);
        }
        Object defaultValue;
        if (pgColumn.defaultValue().isEmpty()) {
            defaultValue = switch (columnType) {
                case ColumnType.INT, ColumnType.BIGINT, ColumnType.DOUBLE -> 0;
                case ColumnType.DATETIME -> new Date(0);
                case ColumnType.BOOLEAN -> false;
                case ColumnType.JSON_OBJECT -> new JSONObject();
                case ColumnType.JSON_ARRAY -> new JSONArray();
                default -> "";
            };
        } else {
            defaultValue = switch (columnType) {
                case ColumnType.INT, ColumnType.BIGINT -> Long.valueOf(pgColumn.defaultValue());
                case ColumnType.DOUBLE -> Double.valueOf(pgColumn.defaultValue());
                case ColumnType.DATETIME -> DateUtils.parse(pgColumn.defaultValue());
                case ColumnType.BOOLEAN -> pgColumn.defaultValue().equalsIgnoreCase("true");
                case ColumnType.JSON_OBJECT -> JsonUtils.parseObject(pgColumn.defaultValue());
                case ColumnType.JSON_ARRAY -> JsonUtils.parseArray(pgColumn.defaultValue());
                default -> pgColumn.defaultValue();
            };
        }
        return new ColumnDefinition(fieldName, columnName, columnType, defaultValue, pgColumn.comment());
    }

    private KeyDefinition getKeyDefinition(Field field) {
        PgKey pgKey = field.getAnnotation(PgKey.class);
        assert pgKey != null;
        ColumnType columnType = EntityUtils.getKeyType(field);
        String fieldName = field.getName();
        String columnName;
        if (StringUtils.hasText(pgKey.column())) {
            columnName = pgKey.column();
        } else {
            columnName = EntityUtils.camelToSnake(fieldName);
        }
        return new KeyDefinition(fieldName, columnName, columnType, pgKey.comment());
    }

    private List<IndexDefinition> getIndexDefinitions(PgIndex[] pgIndexes) {
        List<IndexDefinition> indexDefinitions = Collections.emptyList();
        if (pgIndexes.length > 0) {
            indexDefinitions = new ArrayList<>();
            IndexDefinition indexDefinition;
            String indexName;
            for (PgIndex pgIndex : pgIndexes) {
                indexName = EntityConstants.COMP_INDEX_PREFIX + String.join("_", pgIndex.columns());
                indexDefinition = new IndexDefinition(indexName, pgIndex.unique(), pgIndex.columns());
                indexDefinitions.add(indexDefinition);
            }
        }
        return indexDefinitions;
    }

    public EntityDefinition getEntityDefinition() {
        KeyDefinition keyDefinition = null;
        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
        Field[] fieldArray = this.entityClass.getDeclaredFields();
        for (Field field : fieldArray) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.isAnnotationPresent(PgKey.class)) {
                keyDefinition = this.getKeyDefinition(field);
            } else if (field.isAnnotationPresent(PgColumn.class)) {
                columnDefinitions.add(this.getColumnDefinition(field));
            }
        }

        Assert.notNull(keyDefinition, String.format(EntityConstants.ERROR_KEY_IS_NULL_FORMAT, "PgKey"));
        Assert.notEmpty(columnDefinitions, String.format(EntityConstants.ERROR_COLUMNS_IS_EMPTY_FORMAT, "PgColumn"));

        PgEntity pgEntity = this.entityClass.getAnnotation(PgEntity.class);

        PropertySourcesPlaceholdersResolver propertySourcesPlaceholdersResolver = new PropertySourcesPlaceholdersResolver(this.applicationContext.getEnvironment());

        String dataSource = pgEntity.dataSource();
        if (StringUtils.hasText(dataSource)) {
            dataSource = propertySourcesPlaceholdersResolver.resolvePlaceholders(dataSource).toString();
        }

        String schema = pgEntity.schema();
        if (StringUtils.hasText(schema)) {
            schema = propertySourcesPlaceholdersResolver.resolvePlaceholders(schema).toString();
        }

        String table = pgEntity.table();
        if (table.isEmpty()) {
            table = EntityUtils.getTableName(this.entityClass);
        }

        List<IndexDefinition> indexDefinitions = this.getIndexDefinitions(pgEntity.indexes());
        return new EntityDefinition(dataSource, schema, table, pgEntity.comment(),
                pgEntity.createTableAuto(), pgEntity.createIndexAuto(),
                keyDefinition, columnDefinitions, indexDefinitions);
    }

    private PgEntityDao<?> newInstance() {
        EntityDefinition entityDefinition = this.getEntityDefinition();
        log.info("PgEntity: {}", entityDefinition.table());
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate(entityDefinition.dataSource());

        if (entityDefinition.createTableAuto()) {
            if (StringUtils.hasText(entityDefinition.schema())) {
                String sql = PgSqlUtils.createSchemaIfNotExists(entityDefinition.schema());
                log.debug("execute Postgresql: {}", sql);
                jdbcTemplate.execute(sql);
            }
        }
        //todo
        return new PgEntityDao<Object>() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };
    }
}
