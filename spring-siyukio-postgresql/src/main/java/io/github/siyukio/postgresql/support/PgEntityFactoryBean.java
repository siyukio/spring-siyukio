package io.github.siyukio.postgresql.support;

import io.github.siyukio.postgresql.registrar.PostgresqlEntityRegistrar;
import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityConstants;
import io.github.siyukio.tools.entity.EntityExecutor;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.definition.IndexDefinition;
import io.github.siyukio.tools.entity.definition.KeyDefinition;
import io.github.siyukio.tools.entity.executor.CryptoEntityExecutor;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgIndex;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;
import io.github.siyukio.tools.util.EntityUtils;
import io.github.siyukio.tools.util.XDataUtils;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.RecordComponent;
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
        return this.repository.get();
    }

    @Override
    public Class<?> getObjectType() {
        return PgEntityDao.class;
    }

    @Override
    public void afterPropertiesSet() {
        this.repository = Lazy.of(this::newInstance);
        if (!this.lazyInit) {
            this.repository.get();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    private ColumnDefinition getColumnDefinition(RecordComponent recordComponent) {
        PgColumn pgColumn = recordComponent.getAnnotation(PgColumn.class);
        assert pgColumn != null;
        ColumnType columnType = EntityUtils.getColumnType(recordComponent);
        String fieldName = recordComponent.getName();
        String columnName;
        if (StringUtils.hasText(pgColumn.column())) {
            columnName = pgColumn.column();
        } else {
            columnName = EntityUtils.camelToSnake(fieldName);
        }
        EntityUtils.isSafe(columnName);
        String defaultValueStr = pgColumn.defaultValue();
        Object defaultValue;
        if (defaultValueStr.isEmpty()) {
            defaultValue = switch (columnType) {
                case ColumnType.INT, ColumnType.BIGINT, ColumnType.DOUBLE -> 0;
                case ColumnType.BOOLEAN -> false;
                case ColumnType.JSON_ARRAY -> new JSONArray();
                case ColumnType.JSON_OBJECT -> new JSONObject();
                case ColumnType.TEXT, ColumnType.DATETIME -> "";
            };
        } else {
            defaultValue = switch (columnType) {
                case ColumnType.INT -> Integer.parseInt(defaultValueStr);
                case ColumnType.BIGINT -> Long.parseLong(defaultValueStr);
                case ColumnType.DOUBLE -> Double.parseDouble(defaultValueStr);
                case ColumnType.BOOLEAN -> Boolean.valueOf(defaultValueStr);
                case ColumnType.JSON_ARRAY -> XDataUtils.parseArray(defaultValueStr);
                case ColumnType.JSON_OBJECT -> XDataUtils.parseObject(defaultValueStr);
                case ColumnType.TEXT -> defaultValueStr;
                case ColumnType.DATETIME -> XDataUtils.parse(defaultValueStr);
            };
        }
        boolean encrypted = pgColumn.encrypted();
        if (fieldName.equals(EntityConstants.SALT_COLUMN)) {
            columnType = ColumnType.TEXT;
            defaultValue = "";
            encrypted = false;
        }

        return new ColumnDefinition(fieldName, columnName, columnType, defaultValue,
                encrypted, pgColumn.comment());
    }

    private KeyDefinition getKeyDefinition(RecordComponent recordComponent) {
        PgKey pgKey = recordComponent.getAnnotation(PgKey.class);
        assert pgKey != null;
        ColumnType columnType = EntityUtils.getKeyType(recordComponent);
        String fieldName = recordComponent.getName();
        String columnName;
        if (StringUtils.hasText(pgKey.column())) {
            columnName = pgKey.column();
        } else {
            columnName = EntityUtils.camelToSnake(fieldName);
        }
        EntityUtils.isSafe(columnName);
        return new KeyDefinition(fieldName, columnName, columnType, pgKey.generated(), pgKey.comment());
    }

    private String getIndexName(String table, String[] columns, boolean unique) {
        List<String> list = new ArrayList<>();
        list.add(table);
        list.addAll(List.of(columns));
        String indexName = String.join("_", list);
        if (indexName.length() > 58) {
            indexName = indexName.substring(0, 58);
        }
        if (unique) {
            indexName += "_" + EntityConstants.UNIQUE_INDEX_SUFFIX;
        } else {
            indexName += "_" + EntityConstants.INDEX_SUFFIX;
        }
        return indexName.toLowerCase();
    }

    private List<IndexDefinition> getIndexDefinitions(String table, PgIndex[] pgIndexes) {
        List<IndexDefinition> indexDefinitions = Collections.emptyList();
        if (pgIndexes.length > 0) {
            indexDefinitions = new ArrayList<>();
            IndexDefinition indexDefinition;
            String indexName;
            for (PgIndex pgIndex : pgIndexes) {
                indexName = this.getIndexName(table, pgIndex.columns(), pgIndex.unique());
                EntityUtils.isSafe(indexName);
                indexDefinition = new IndexDefinition(indexName, pgIndex.columns(), pgIndex.unique());
                indexDefinitions.add(indexDefinition);
            }
        }
        return indexDefinitions;
    }

    public EntityDefinition getEntityDefinition() {
        KeyDefinition keyDefinition = null;
        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
        RecordComponent[] recordComponents = this.entityClass.getRecordComponents();
        boolean encrypted = false;
        ColumnDefinition columnDefinition;
        for (RecordComponent recordComponent : recordComponents) {
            XDataUtils.checkType(this.entityClass, recordComponent.getType());
            if (recordComponent.isAnnotationPresent(PgKey.class)) {
                keyDefinition = this.getKeyDefinition(recordComponent);
            } else if (recordComponent.isAnnotationPresent(PgColumn.class)) {
                columnDefinition = this.getColumnDefinition(recordComponent);
                if (columnDefinition.encrypted()) {
                    encrypted = true;
                }
                columnDefinitions.add(columnDefinition);
            }
        }

        if (encrypted) {
            List<ColumnDefinition> items = columnDefinitions.stream()
                    .filter(item -> item.fieldName().equals(EntityConstants.SALT_COLUMN))
                    .toList();
            Assert.notEmpty(items, String.format(EntityConstants.ERROR_SALT_COLUMN_MISSING_FORMAT, this.entityClass.getSimpleName(), EntityConstants.SALT_COLUMN));
        }

        Assert.notNull(keyDefinition, String.format(EntityConstants.ERROR_KEY_IS_NULL_FORMAT, this.entityClass.getSimpleName()));
        Assert.notEmpty(columnDefinitions, String.format(EntityConstants.ERROR_COLUMNS_IS_EMPTY_FORMAT, this.entityClass.getSimpleName()));

        PgEntity pgEntity = this.entityClass.getAnnotation(PgEntity.class);

        PropertySourcesPlaceholdersResolver propertySourcesPlaceholdersResolver = new PropertySourcesPlaceholdersResolver(this.applicationContext.getEnvironment());

        String dbName = pgEntity.dbName();
        if (StringUtils.hasText(dbName)) {
            dbName = propertySourcesPlaceholdersResolver.resolvePlaceholders(dbName).toString();
        }

        String schema = pgEntity.schema();
        if (StringUtils.hasText(schema)) {
            schema = propertySourcesPlaceholdersResolver.resolvePlaceholders(schema).toString();
            EntityUtils.isSafe(schema);
        } else {
            schema = PgSqlUtils.DEFAULT_SCHEMA;
        }

        String table = pgEntity.table();
        if (table.isEmpty()) {
            table = EntityUtils.getTableName(this.entityClass);
        }

        EntityUtils.isSafe(table);

        String keyInfo = pgEntity.keyInfo();
        if (!StringUtils.hasText(keyInfo)) {
            keyInfo = this.entityClass.getSimpleName();
        }

        List<IndexDefinition> indexDefinitions = this.getIndexDefinitions(table, pgEntity.indexes());
        return new EntityDefinition(dbName, schema, table, pgEntity.comment(),
                pgEntity.createTableAuto(), pgEntity.addColumnAuto(), pgEntity.createIndexAuto(),
                encrypted, keyInfo,
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
            return XDataUtils.copy(rowJson, InformationIndex.class);
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
            log.info("createIndex: {}, {}", entityDefinition.schema(), sqlList);
            this.executeSqlScript("createIndex", entityDefinition.dbName(), sqlList);
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
            return XDataUtils.copy(rowJson, InformationColumn.class);
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
                case ColumnType.DATETIME -> udtName.equalsIgnoreCase("text");
                case ColumnType.JSON_ARRAY, ColumnType.JSON_OBJECT -> udtName.equalsIgnoreCase("json");
                default -> columnDefinition.type().name().equalsIgnoreCase(informationColumn.udtName());
            };

            if (!isSameType) {
                sqlList.add(PgSqlUtils.alterColumnTypeSql(entityDefinition, columnDefinition));
            }

            String columnDefault = informationColumn.columnDefault();
            if (columnDefault == null) {
                sqlList.add(PgSqlUtils.alterColumnDefaultSql(entityDefinition, columnDefinition));
            } else {
                if (StringUtils.hasText(columnDefault)) {
                    int index = columnDefault.indexOf("::");
                    if (index > 0) {
                        columnDefault = columnDefault.substring(0, index);
                    }
                }
                String defaultValue = PgSqlUtils.getSqlDefault(columnDefinition);
                if (!columnDefault.equals(defaultValue)) {
                    sqlList.add(PgSqlUtils.alterColumnDefaultSql(entityDefinition, columnDefinition));
                }
            }
        }
        return sqlList;
    }

    private void executeSqlScript(String title, String dbName, List<String> sqlList) {
        String sql = String.join(System.lineSeparator(), sqlList);
        MultiJdbcTemplate multiJdbcTemplate = PostgresqlEntityRegistrar.getMultiJdbcTemplate(dbName);
        DataSource dataSource = multiJdbcTemplate.getDataSource();

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
            log.debug("alterTable: {}, {}", entityDefinition.schema(), sqlList);
            this.executeSqlScript("alterTable", entityDefinition.dbName(), sqlList);
        }
    }

    private void createTable(EntityDefinition entityDefinition) {
        if (!entityDefinition.createTableAuto()) {
            return;
        }
        List<String> sqlList = PgSqlUtils.createTableAndCommentSql(entityDefinition);
        log.info("createTable: {}, {}", entityDefinition.schema(), sqlList);
        this.executeSqlScript("createTable", entityDefinition.dbName(), sqlList);
    }

    private void checkTable(EntityDefinition entityDefinition, JdbcTemplate jdbcTemplate) {

        if (StringUtils.hasText(entityDefinition.schema()) && !SCHEMA_SET.contains(entityDefinition.schema())) {
            String sql = PgSqlUtils.createSchemaIfNotExistsSql(entityDefinition.schema());
            log.info("checkSchema: {}", sql);
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
        MultiJdbcTemplate multiJdbcTemplate = PostgresqlEntityRegistrar.getMultiJdbcTemplate(entityDefinition.dbName());
        this.checkTable(entityDefinition, multiJdbcTemplate.getMaster());

        EntityExecutor entityExecutor = new PgEntityExecutor(entityDefinition, multiJdbcTemplate);
        if (entityDefinition.encrypted()) {
            entityExecutor = new CryptoEntityExecutor(entityExecutor);
        }
        return new PgEntityDaoImpl<>(this.entityClass, entityExecutor);
    }
}
