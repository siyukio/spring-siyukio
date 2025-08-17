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
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Bugee
 */
@Slf4j
public class PgEntityFactoryBean implements FactoryBean<PgEntityDao<?>>, InitializingBean, ApplicationContextAware {

    private final static Map<String, JdbcTemplate> JDBC_TEMPLATE_MAP = new HashMap<>();

    private final static Set<String> SCHEMA_SET = new HashSet<>();

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

    private DataSource getDataSource(String dataSourceName) {
        Map<String, DataSource> dataSourceMap = this.applicationContext.getBeansOfType(DataSource.class);
        DataSource dataSource = dataSourceMap.get(dataSourceName);
        Assert.notNull(dataSource, String.format(EntityConstants.ERROR_DATASOURCE_IS_NULL_FORMAT, dataSourceName));
        return dataSource;
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
        EntityUtils.isSafe(columnName);
        String defaultValue = pgColumn.defaultValue();
        if (defaultValue.isEmpty()) {
            defaultValue = switch (columnType) {
                case ColumnType.INT, ColumnType.BIGINT, ColumnType.DOUBLE -> "0";
                case ColumnType.DATETIME -> DateUtils.format(new Date(0));
                case ColumnType.BOOLEAN -> "false";
                case ColumnType.JSON_OBJECT -> "{}";
                case ColumnType.JSON_ARRAY -> "[]";
                default -> "";
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
        EntityUtils.isSafe(columnName);
        return new KeyDefinition(fieldName, columnName, columnType, pgKey.generated(), pgKey.comment());
    }

    private List<IndexDefinition> getIndexDefinitions(PgIndex[] pgIndexes) {
        List<IndexDefinition> indexDefinitions = Collections.emptyList();
        if (pgIndexes.length > 0) {
            indexDefinitions = new ArrayList<>();
            IndexDefinition indexDefinition;
            String indexName;
            for (PgIndex pgIndex : pgIndexes) {
                indexName = EntityConstants.COMP_INDEX_PREFIX + String.join("_", pgIndex.columns());
                EntityUtils.isSafe(indexName);
                indexDefinition = new IndexDefinition(indexName, pgIndex.columns());
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
            EntityUtils.isSafe(schema);
        }

        String table = pgEntity.table();
        if (table.isEmpty()) {
            table = EntityUtils.getTableName(this.entityClass);
        }

        EntityUtils.isSafe(table);

        List<IndexDefinition> indexDefinitions = this.getIndexDefinitions(pgEntity.indexes());
        return new EntityDefinition(dataSource, schema, table, pgEntity.comment(),
                pgEntity.createTableAuto(), pgEntity.addColumnAuto(), pgEntity.createIndexAuto(),
                keyDefinition, columnDefinitions, indexDefinitions);
    }

    private Map<String, InformationIndex> queryIndexes(EntityDefinition entityDefinition, JdbcTemplate jdbcTemplate) {
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = PgSqlUtils.DEFAULT_SCHEMA;
        }

        List<InformationIndex> informationIndexes = jdbcTemplate.query(PgSqlUtils.QUERY_INDEXES_SQL, (rs, rowNum) -> {
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            JSONObject rowJson = new JSONObject();
            for (int index = 1; index <= columnCount; index++) {
                rowJson.put(resultSetMetaData.getColumnLabel(index), rs.getObject(index));
            }
            return JsonUtils.copy(rowJson, InformationIndex.class);
        }, schema, entityDefinition.table());

        return informationIndexes.stream()
                .collect(Collectors.toMap(
                        InformationIndex::indexName,
                        Function.identity()
                ));
    }

    private void createIndex(EntityDefinition entityDefinition, JdbcTemplate jdbcTemplate) {
        if (!entityDefinition.createIndexAuto() || entityDefinition.indexDefinitions().isEmpty()) {
            return;
        }
        List<String> sqlList = new ArrayList<>();
        Map<String, InformationIndex> informationIndexMap = this.queryIndexes(entityDefinition, jdbcTemplate);
        for (IndexDefinition indexDefinition : entityDefinition.indexDefinitions()) {
            if (!informationIndexMap.containsKey(indexDefinition.indexName())) {
                sqlList.add(PgSqlUtils.createIndexSql(entityDefinition, indexDefinition));
            }
        }
        if (!sqlList.isEmpty()) {
            this.executeSqlScript("createIndex", entityDefinition.dataSource(), sqlList);
        }
    }

    private Map<String, InformationColumn> queryColumns(EntityDefinition entityDefinition, JdbcTemplate jdbcTemplate) {
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = PgSqlUtils.DEFAULT_SCHEMA;
        }

        List<InformationColumn> informationColumns = jdbcTemplate.query(PgSqlUtils.QUERY_COLUMNS_SQL, (rs, rowNum) -> {
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            JSONObject rowJson = new JSONObject();
            for (int index = 1; index <= columnCount; index++) {
                rowJson.put(resultSetMetaData.getColumnLabel(index), rs.getObject(index));
            }
            return JsonUtils.copy(rowJson, InformationColumn.class);
        }, schema, entityDefinition.table());

        return informationColumns.stream()
                .collect(Collectors.toMap(
                        InformationColumn::columnName,
                        Function.identity()
                ));
    }

    private List<String> checkColumn(EntityDefinition entityDefinition, ColumnDefinition columnDefinition, Map<String, InformationColumn> informationColumnMap) {
        List<String> sqlList;
        InformationColumn informationColumn = informationColumnMap.get(columnDefinition.columnName());
        if (informationColumn == null) {
            // add column
            sqlList = PgSqlUtils.addColumnAndCommentSql(entityDefinition, columnDefinition);
        } else {
            sqlList = new ArrayList<>();
            String udtName = informationColumn.udtName();
            boolean isSameType = switch (columnDefinition.type()) {
                case ColumnType.BOOLEAN -> udtName.equalsIgnoreCase("bool");
                case ColumnType.INT -> udtName.equalsIgnoreCase("int4");
                case ColumnType.BIGINT -> udtName.equalsIgnoreCase("int8");
                case ColumnType.DOUBLE -> udtName.equalsIgnoreCase("float8");
                case ColumnType.DATETIME -> udtName.equalsIgnoreCase("timestamp");
                case ColumnType.JSON_ARRAY, ColumnType.JSON_OBJECT -> udtName.equalsIgnoreCase("json");
                default -> columnDefinition.type().name().equalsIgnoreCase(informationColumn.udtName());
            };

            if (!isSameType) {
                sqlList.add(PgSqlUtils.alterColumnTypeSql(entityDefinition, columnDefinition));
            }

            String columnDefault = informationColumn.columnDefault();
            int index = columnDefault.indexOf("::");
            if (index > 0) {
                columnDefault = columnDefault.substring(0, index);
            }
            columnDefault = columnDefault.replaceAll("'", "");
            String defaultValue = columnDefinition.defaultValue();
            if (!columnDefault.equals(defaultValue)) {
                sqlList.add(PgSqlUtils.alterColumnDefaultSql(entityDefinition, columnDefinition));
            }
        }
        return sqlList;
    }

    private void executeSqlScript(String title, String dataSourceName, List<String> sqlList) {
        String sql = String.join(System.lineSeparator(), sqlList);
        log.debug("{} Postgresql: {}", title, sql);
        DataSource dataSource = this.getDataSource(dataSourceName);

        try (Connection conn = dataSource.getConnection()) {
            ByteArrayResource resource = new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, resource);
        } catch (SQLException e) {
            log.error("{} Postgresql error", title, e);
        }
    }

    private void alterTable(EntityDefinition entityDefinition, Map<String, InformationColumn> informationColumnMap) {
        if (!entityDefinition.addColumnAuto()) {
            return;
        }
        List<String> sqlList = new ArrayList<>();
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            sqlList.addAll(this.checkColumn(entityDefinition, columnDefinition, informationColumnMap));
        }
        if (!sqlList.isEmpty()) {
            this.executeSqlScript("alterTable", entityDefinition.dataSource(), sqlList);
        }
    }

    private void createTable(EntityDefinition entityDefinition) {
        if (!entityDefinition.createTableAuto()) {
            return;
        }
        List<String> sqlList = PgSqlUtils.createTableAndCommentSql(entityDefinition);
        this.executeSqlScript("createTable", entityDefinition.dataSource(), sqlList);
    }

    private void checkTable(EntityDefinition entityDefinition, JdbcTemplate jdbcTemplate) {

        if (StringUtils.hasText(entityDefinition.schema()) && !SCHEMA_SET.contains(entityDefinition.schema())) {
            String sql = PgSqlUtils.createSchemaIfNotExistsSql(entityDefinition.schema());
            log.debug("checkTable Postgresql: {}", sql);
            jdbcTemplate.execute(sql);
            SCHEMA_SET.add(entityDefinition.schema());
        }

        if (entityDefinition.createTableAuto() || entityDefinition.addColumnAuto()) {
            Map<String, InformationColumn> informationColumnMap = this.queryColumns(entityDefinition, jdbcTemplate);
            if (informationColumnMap.isEmpty()) {
                this.createTable(entityDefinition);
            } else {
                this.alterTable(entityDefinition, informationColumnMap);
            }
        }

        this.createIndex(entityDefinition, jdbcTemplate);
    }

    private PgEntityDao<?> newInstance() {
        EntityDefinition entityDefinition = this.getEntityDefinition();
        log.info("PgEntity: {}", entityDefinition.table());
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate(entityDefinition.dataSource());

        this.checkTable(entityDefinition, jdbcTemplate);

        //todo
        return new PgEntityDao<Object>() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };
    }
}
