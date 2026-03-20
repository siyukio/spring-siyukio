# spring-siyukio-postgresql

## Module Purpose

spring-siyukio-postgresql provides PostgreSQL entity management with automatic schema synchronization. It defines
PostgreSQL entities using annotations and automatically creates or updates tables, columns, and indexes when the entity
structure differs from the database.

## Key Highlights

- **Zero-Configuration Schema Sync**: Automatically detects and executes DDL to create/update tables, columns, and indexes on DAO initialization—no manual schema management required
- **Column-Level AES-GCM Encryption**: Secure sensitive data with automatic encryption/decryption using derived keys from master key + per-record salt + entity keyInfo
- **Built-in Audit Timestamps**: Auto-manage `createdAt`, `createdAtTs`, `updatedAt`, `updatedAtTs` fields without manual intervention
- **Complex Object Mapping**: Seamlessly handle nested records, enums, lists, JSONObject, and JSONArray—no extra serialization code needed
- **Automatic Time-Based Partitioning**: Support YEAR/MONTH/DAY/HOUR partition strategies with automatic partition creation and monitoring
- **Master-Slave Architecture**: Built-in support for one master with multiple slaves for high availability and read scalability
- **Spring-Native Integration**: Full compatibility with Spring Boot `@Transactional`, automatic bean injection, and Spring ecosystem

## Core Features

- **Entity Definition Annotations**:
    - `@PgEntity`: Defines the table with schema, partition strategy, and index configurations
    - `@PgKey`: Defines the primary key component with auto-generation support
    - `@PgColumn`: Defines column properties including encryption
    - `@PgIndex`: Defines indexes on one or more columns

- **Automatic Schema Synchronization**: On `PgEntityDao<T>` initialization, detects differences between entity structure
  and database, automatically executing DDL statements to create tables, columns, and indexes

- **Complex Object Mapping**: Automatically converts complex objects (nested records, enums, lists, JSONObject,
  JSONArray) to/from database storage

- **Bean Injection Support**: `PgEntityDao<T>` is a Spring bean that supports automatic injection

- **Multi-Database Configuration**: Supports one master with multiple slaves configuration

- **Spring Boot Transaction Support**: Compatible with Spring Boot `@Transactional` annotation for transaction
  management

- **Column-Level Encryption**: Supports specifying columns for encrypted storage

- **Automatic Partitioning**: Supports automatic table partitioning by YEAR, MONTH, DAY, or HOUR

## Usage

### Configuration

Configure PostgreSQL datasource with master-slave setup and encryption key:

The `master-key` is used for reversible encryption of columns marked with `@PgColumn(encrypted = true)`:

```yaml
spring:
  datasource:
    postgres:
      master-key: 06CVrBQL+6VZzbXYhxfXYIm40I/cS4Ern2DW7beR5JU=
      hikari:
        maximum-pool-size: 20
        minimum-idle: 20
        idle-timeout: 600000
        max-lifetime: 1800000
      master:
        url: jdbc:postgresql://localhost:5432/root
        username: root
        password: ${POSTGRES_PASSWORD}
      slaves:
        - url: jdbc:postgresql://localhost:5432/root
          username: root
          password: ${POSTGRES_PASSWORD}
```

### Entity Definition

Define a PostgreSQL entity record with annotations:

```java

@PgEntity(schema = "test", comment = "demo entity",
        indexes = {
                @PgIndex(columns = {"userId"}, unique = false)
        })
@Builder
@With
public record DemoEntity(

        @PgKey
        String id,

        @PgColumn(encrypted = true)
        String encryptContent,

        @PgColumn
        String salt,

        @PgColumn
        boolean error,

        @PgColumn
        double rating,

        @PgColumn
        int total,

        @PgColumn
        String userId,

        @PgColumn
        JSONObject metadata,

        @PgColumn
        JSONArray messages,

        @PgColumn(comment = "item")
        Item item,

        @PgColumn(comment = "items")
        List<Item> items,

        @PgColumn
        LoginType loginType,

        @PgColumn
        LocalDateTime createdAt,

        @PgColumn
        long createdAtTs,

        @PgColumn
        LocalDateTime updatedAt,

        @PgColumn
        long updatedAtTs
) {
    @EnumNaming(value = EnumNamingStrategies.LowerCamelCaseStrategy.class)
    public enum LoginType {
        USERNAME,
        PHONE,
        EMAIL,
        GOOGLE,
        APPLE
    }

    @Builder
    @With
    public record Item(
            String type,
            long costMs
    ) {
    }
}
```

### Column-Level Encryption

To enable reversible encryption for sensitive columns, use `@PgColumn(encrypted = true)` and include a `salt` field:

**Key Derivation Process**:

- `master-key` is a 256-bit HMAC-SHA256 master key, unique within a datasource
- Each entity can configure `keyInfo` via `@PgEntity(keyInfo = "...")`. If not configured, defaults to the entity class
  `getSimpleName()`
- `salt` is randomly generated as 128-bit on insert, different for each record in the same table
- `salt` and `keyInfo` form the context-specific information
- AES-GCM key is derived from the master key using HMAC-SHA256 with the context (salt + keyInfo)
- Each encrypted column uses a unique derived key based on its record's salt and entity's keyInfo

**Encryption/Decryption**:

- Encryption happens automatically during insert/update operations
- Decryption happens automatically during query operations

**Important**: When using encrypted columns, the entity must include a `salt` field for key derivation.

### Entity Dao Operations

Inject `PgEntityDao<T>` and perform CRUD operations:

**Auto-updated Timestamps**:

- `createdAt`: Record creation time in string format, auto-set on insert
- `createdAtTs`: Record creation time in milliseconds (long), auto-set on insert
- `updatedAt`: Last update time in string format, auto-set on insert and update
- `updatedAtTs`: Last update time in milliseconds (long), auto-set on insert and update

It is recommended to include these 4 timestamp fields in every entity for audit purposes. The DAO automatically updates these fields based on the current operation.

```java
public void demo() {
    // Insert
    DemoEntity entity = DemoEntity.builder()
            .type("user")
            .encryptContent("test")
            .build();
    recordEventPgEntityDao.insert(entity);

    // Query by id
    entity = recordEventPgEntityDao.queryById(id);

    // Update
    entity = entity.withType("updated");
    recordEventPgEntityDao.update(entity);

    // Upsert
    recordEventPgEntityDao.upsert(entity);

    // Delete
    recordEventPgEntityDao.deleteById(id);

    // Query with conditions
    QueryBuilder queryBuilder = QueryBuilders.termQuery("type", "user");
    List<DemoEntity> entities = recordEventPgEntityDao.queryList(queryBuilder, 0, 10);

    // Query with sort
    SortBuilder sortBuilder = SortBuilders.fieldSort("createdAtTs").order(SortOrder.DESC);
    entities = recordEventPgEntityDao.queryList(queryBuilder, sortBuilder, 0, 10);

    // Page query
    Page<DemoEntity> page = recordEventPgEntityDao.queryPage(queryBuilder, sortBuilder, 1, 10);

    // Count
    int count = recordEventPgEntityDao.queryCount();
    count = recordEventPgEntityDao.queryCount(queryBuilder);
}
```

### Partitioned Table

Define a partitioned entity by time:

```java

@PgEntity(schema = "test", comment = "partitioned demo entity",
        partition = EntityDefinition.Partition.HOUR,
        indexes = {
                @PgIndex(columns = {"message", "createdAtTs"})
        })
@Builder
@With
public record DemoPartitionedEntity(

        @PgKey
        String id,

        @PgColumn
        String message,

        @PgColumn
        String salt,

        @PgColumn
        LocalDateTime createdAt,

        @PgColumn
        long createdAtTs,

        @PgColumn
        LocalDateTime updatedAt,

        @PgColumn
        long updatedAtTs
) {
}
```

For partitioned tables, the partition is automatically created and checked every 6 minutes. The table requires a
`createdAtTs` field for partitioning.

**Note**: Partitioned table operations are the same as normal tables.

---

[← Back to Main Documentation](../README.md)
