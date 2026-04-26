---
name: siyukio-create-domain
description: "Generate and modify domain model (Entity) for Siyukio-based Spring Boot applications using PostgreSQL"
triggers:
  - "add entity"
  - "create entity"
  - "new entity"
  - "modify entity"
  - "update entity"
  - "add field"
  - "add index"
  - "entity add"
  - "entity create"
  - "entity modify"
  - "add policy"
  - "create policy"
  - "check entity policy"
  - "add errors"
  - "create errors"
---

<Purpose>
Generate and modify domain model (Entity) classes for Siyukio Spring Boot applications using PostgreSQL. This is a fundamental programming skill that creates pure Java record classes with Siyukio persistence annotations.
</Purpose>

<Use_When>

- Creating a new Entity class
- Modifying existing Entity (add/update/remove fields)
- Adding indexes to an Entity
- Defining nested records within an Entity

</Use_When>

<Prerequisites>
- Target domain module must exist: `{project-name}/{project-name}-domain-{domain}/`
- Module must have `spring-siyukio-postgresql` dependency in pom.xml
- Target file location: `src/main/java/{package-path}/{domain}/domain/model/{Entity}.java`
</Prerequisites>

<Execution_Protocol>

## Step 1: Determine Entity Requirements

From the argument, extract:

- `{Domain}`: Domain module name (kebab-case, e.g., `user-management`)
- `{Entity}`: Entity name (PascalCase, e.g., `User`)
- `{entity}`: Entity variable name (camelCase, e.g., `user`)
- `{schema}`: PostgreSQL schema name (e.g., `public`, `user_mgmt`)
- `{tableName}`: Database table name (snake_case, e.g., `user_account`)
- Fields: name, type, constraints, whether encrypted, nested records
- Indexes: columns, unique constraint

## Step 2: Verify/Add Maven Dependency

Ensure `{project-name}-domain-{domain}/pom.xml` has:

```xml
<dependency>
    <groupId>io.github.siyukio</groupId>
    <artifactId>spring-siyukio-postgresql</artifactId>
</dependency>
```

## Step 3: Generate Entity Record

Location: `{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/domain/model/{Entity}.java`

### Complete Entity Template

```java
package {package-name}.{domain}.domain.model;

import io.github.siyukio.postgresql.entity.annotation.PgColumn;
import io.github.siyukio.postgresql.entity.annotation.PgEntity;
import io.github.siyukio.postgresql.entity.annotation.PgIndex;
import io.github.siyukio.postgresql.entity.annotation.PgKey;
import lombok.Builder;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Entity representing {Entity}.
 */
@Builder
@With
@PgEntity(comment = "{tableName}",
        indexes = {
                @PgIndex(columns = {"type"}),
                @PgIndex(columns = {"teamId", "userId"}, unique = true)
        })
public record {Entity}(

        @PgKey
        String id,

        @PgColumn
        String name,

        @PgColumn
        String type,

        @PgColumn
        String description,

        @PgColumn(encrypted = true)
        String secretValue,

        @PgColumn
        boolean enabled,

        @PgColumn
        String teamId,

        @PgColumn
        String userId,

        @PgColumn
        LocalDateTime createdAt,

        @PgColumn
        LocalDateTime updatedAt

) {
    // Nested records for complex fields
    // @Builder @With public record Item(String type, long costMs) {}

    // Inner enums for fixed values
    // public enum Status { ACTIVE, INACTIVE }
}
```

## Step 5: Generate Errors Interface (Optional but Recommended)

Location: `{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/domain/errors/{Entity}Errors.java`

Create an interface to define error message templates used by the corresponding Policy:

```java
package {package-name}.{domain}.domain.errors;

/**
 * Error message templates for {Entity} domain.
 */
public interface {Entity}Errors {

    String { ENTITY_NAME }_NOT_FOUND = "{Entity} not found: %s";
    String { ENTITY_NAME }_ALREADY_EXISTS = "{Entity} with id '%s' already exists";
    String { ENTITY_NAME }_DISABLED = "{Entity} is disabled: %s";
}
```

> **Note:** Replace `{ ENTITY_NAME }` with the entity name in uppercase (e.g., `SKILL`, `USER`).

## Step 6: Generate Policy Class (Optional but Recommended)

Location: `{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/domain/policy/{Entity}Policy.java`

Create a Policy class to encapsulate entity validation and query operations:

```java
package {package-name}.{domain}.domain.policy;

import {package-name}.{domain}.domain.errors.{Entity}Errors;
import {package-name}.{domain}.domain.model.{Entity};
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Policy class for {Entity} validation and query operations.
 */
@Component
public class {Entity}Policy {

    @Autowired
    private PgEntityDao<{Entity}> {entity}Dao;

    /**
     * Check if {entity} exists by id.
     *
     * @param id the entity id
     * @return the entity if found
     * @throws ApiException if entity not found
     */
    public {Entity} check{Entity}Exists(String id) {
        {Entity} {entity} = this.{entity}Dao.queryById(id);
        if ({entity} == null) {
            throw new ApiException(String.format({Entity}Errors.{ ENTITY_NAME }_NOT_FOUND, id));
        }
        return {entity};
    }

    /**
     * Check if {entity} exists and is enabled.
     *
     * @param id the entity id
     * @return the entity if found and enabled
     * @throws ApiException if entity not found or disabled
     */
    public {Entity} check{Entity}Enabled(String id) {
        {Entity} {entity} = this.check{Entity}Exists(id);
        if (!{entity}.enabled()) {
            throw new ApiException(String.format({Entity}Errors.{ ENTITY_NAME }_DISABLED, id));
        }
        return {entity};
    }

    /**
     * Find {entity} by id, returns null if not found.
     *
     * @param id the entity id
     * @return the entity or null
     */
    public {Entity} findById(String id) {
        return this.{entity}Dao.queryById(id);
    }
}
```

### Common Policy Patterns

| Pattern                   | Purpose                                                |
| ------------------------- | ------------------------------------------------------ |
| `check{Entity}Exists(id)`  | Verify entity exists, throw exception if not           |
| `check{Entity}Enabled(id)` | Verify entity exists and `enabled()` returns true     |
| `findById(id)`            | Query entity, return null if not found (no exception)  |

## Step 7: CRUD Operations Reference

See `siyukio-create-application` skill for Application Service patterns. Domain Policy only contains query/validation methods.

| Policy Method | Purpose                                           |
| ------------- | ------------------------------------------------- |
| `checkExists` | Verify entity exists, throw `ApiException` if not |
| `checkEnabled`| Verify entity exists and `enabled() == true`      |
| `findById`    | Query entity, return `null` if not found          |

</Execution_Protocol>

<Key_Conventions>

| Item              | Convention                                           |
| ----------------- | ---------------------------------------------------- |
| Package           | `{package-name}.{domain}.domain`                     |
| Package Path      | `{package-path}/{domain}/domain/`                    |
| Entity Package    | `{package-name}.{domain}.domain.model`              |
| Errors Package    | `{package-name}.{domain}.domain.errors`             |
| Policy Package    | `{package-name}.{domain}.domain.policy`             |
| Entity Class type | **Must use Java record** (immutable data carrier)    |
| Errors Class type | **Must use Java interface** with `String` constants  |
| Policy Class type | **Must use `@Component`** class with `@Autowired` DAO |
| Table annotation  | `@PgEntity(comment = "...")` — schema/table can use defaults |
| Primary key       | `@PgKey` on the id field                             |
| Column annotation | `@PgColumn` with optional `encrypted`, `comment`     |
| Indexes           | `@PgIndex` within `@PgEntity`                        |
| Nested records    | Define as static inner records with `@Builder` and `@With` |
| Enum fields       | Define as inner enum with `@PgColumn` on the field  |
| JSON typed fields | Dynamic JSON: `org.json.JSONObject` / `org.json.JSONArray` |
|                   | Static JSON: nested record class (framework auto-converts) |
| Timestamps        | `createdAt`, `createdAtTs`, `updatedAt`, `updatedAtTs` (auto) |
| Encrypted fields  | `@PgColumn(encrypted = true)` requires `salt` field (auto) |
| DAO type          | `PgEntityDao<T>` for all CRUD operations             |
| Lombok            | Use `@Builder`, `@With`                              |

</Key_Conventions>

<PgEntityDao_Methods>

### Create Operations

| Method                              | Description                                                             |
| ----------------------------------- | ---------------------------------------------------------------------- |
| `insert(T t)`                       | Insert entity, returns inserted entity (may include generated fields) |
| `insertBatch(Collection<T> tList)`  | Batch insert, returns rows affected                                    |

### Update Operations

| Method                              | Description                                        |
| ----------------------------------- | -------------------------------------------------- |
| `update(T t)`                       | Update entity by identity, returns updated entity  |
| `updateBatch(Collection<T> tList)`  | Batch update, returns rows affected                |
| `upsert(T t)`                       | Insert if not exists, otherwise update             |

### Delete Operations

| Method                                      | Description                                 |
| ------------------------------------------- | ------------------------------------------- |
| `deleteById(Object id)`                     | Delete by primary key, returns rows affected |
| `delete(T t)`                               | Delete by entity, returns rows affected     |
| `deleteByQuery(QueryBuilder queryBuilder)`  | Delete by query, returns rows affected      |

### Query Operations

| Method                                                      | Description                    |
| ---------------------------------------------------------- | ------------------------------ |
| `existById(Object id)`                                      | Check if record exists by ID  |
| `queryById(Object id)`                                      | Get entity by primary key      |
| `queryOne(QueryBuilder queryBuilder)`                       | Get single entity by query     |
| `queryList(QueryBuilder queryBuilder)`                      | Get list by query              |
| `queryList(QueryBuilder, SortBuilder)`                      | Get sorted list                |
| `queryList(QueryBuilder, SortBuilder, int from, int size)`  | Get sorted paginated list      |
| `queryList(int from, int size)`                             | Get paginated list (no filter) |
| `queryList(SortBuilder sort)`                               | Get all with sort              |
| `queryCount()`                                              | Count all records              |
| `queryCount(QueryBuilder queryBuilder)`                     | Count by query                 |
| `queryPage(QueryBuilder, SortBuilder, int page, int size)`  | Get paged result with metadata |

### Supporting Classes

| Class           | Purpose                                                    |
| --------------- | ---------------------------------------------------------- |
| `QueryBuilder`  | Build WHERE conditions for queries                         |
| `SortBuilder`   | Build ORDER BY clauses                                     |
| `QueryBuilders` | Factory for query builders (`QueryBuilders.termQuery(...)`) |
| `SortBuilders`  | Factory for sort builders (`SortBuilders.fieldSort(...)`)  |
| `SortOrder`     | Sort direction: `ASC` or `DESC`                            |
| `Page<T>`       | Paged result with metadata                                 |

</PgEntityDao_Methods>

<Annotation_Reference>

### @PgEntity

Most properties have sensible defaults. Only set when needed.

| Property          | Default | Description                                      |
| ----------------- | ------- | ------------------------------------------------ |
| `comment`         | (none)  | **Required.** Table comment/description          |
| `schema`          | ""      | Schema name (empty = default, typically `public`) |
| `table`           | ""      | Table name (empty = class name in snake_case)    |
| `createTableAuto` | true    | Auto-create table if not exists                  |
| `addColumnAuto`   | true    | Auto-add columns if not exist                    |
| `createIndexAuto` | true    | Auto-create indexes if not exist                 |
| `dbName`          | ""      | Database name for multi-db support               |
| `partition`       | NONE    | Partition strategy: NONE, YEAR, MONTH, DAY, HOUR |
| `keyInfo`         | ""      | Context for encryption key derivation            |
| `indexes`         | {}      | Index definitions                                |
| `cacheConfig`     | @CacheConfig(maximumSize = 0) | Cache settings (see below)               |

### @PgEntity cacheConfig

When `CacheConfig.maximumSize() > 0`, entity-level caching is enabled using Caffeine cache.

|| Property             | Default        | Description                                               |
| -------------------- | -------------- | --------------------------------------------------------- |
| `maximumSize`        | 0              | Max cache entries. **0 = caching disabled**              |
| `softValues`         | false          | Use soft values (GC on low memory)                        |
| `expireUnit`         | MINUTES        | Time unit for expiration                                  |
| `expireAfterAccess`  | 60             | Minutes before entry expires after last access           |
| `expireAfterWrite`   | 15             | Minutes before entry expires after write                  |

```java
// Minimal: only comment is required
@PgEntity(comment = "user account table")
public record User(String id, String name) {}

// Entity with caching enabled (1000 entries, 30min access expire)
@PgEntity(comment = "user account table",
          cacheConfig = @CacheConfig(maximumSize = 1000, expireAfterAccess = 30))
public record User(String id, String email, String name, boolean active) {}

// Full configuration
@PgEntity(schema = "user_mgmt", table = "user_account", comment = "user account table",
          partition = EntityDefinition.Partition.NONE,
          indexes = {
              @PgIndex(columns = {"email"}, unique = true),
              @PgIndex(columns = {"status", "createdAt"}, unique = false)
          },
          keyInfo = "user-encryption-context")
public record User(String id, String email, String name, boolean active) {}
```

### @PgKey

```java
@PgKey  // Auto-generates ID using GUID.v7() base64 (default: generated = true)
String id;

// Manual ID generation (rarely needed)
@PgKey(generated = false)
String id;
```

| Property    | Default | Description                                            |
| ----------- | ------- | ------------------------------------------------------ |
| `generated` | true    | Auto-generate ID using GUID.v7() base64 when inserting |
| `comment`   | ""      | Column comment                                         |

### @PgColumn

| Property    | Default | Description              |
| ----------- | ------- | ------------------------ |
| `encrypted` | false   | Encrypt the column value |
| `comment`   | ""      | Column comment            |

```java
@PgColumn                              // Basic column
@PgColumn(encrypted = true)            // Encrypted column
@PgColumn(comment = "description")     // Column with comment
```

### Field Name Mapping

Field names are automatically mapped to database column names using camelCase to snake_case conversion. No explicit `column` attribute is needed in most cases.

| Field Name    | Column Name     |
| ------------- | --------------- |
| `userName`    | `user_name`     |
| `createdAtTs` | `created_at_ts` |
| `id`          | `id`            |

### Supported Field Types

- Primitives: `String`, `boolean`, `int`, `long`, `double`
- Java time: `LocalDateTime` or `long` (choose one for timestamp fields)
- JSON: `org.json.JSONObject`, `org.json.JSONArray`
- Collections: `List<T>`, `Set<T>`
- Enums: Inner enum types
- Nested records: Inner record types (with `@Builder` and `@With`)

</Annotation_Reference>

<Verification>
After implementation:
1. Run `./mvnw compile` to verify code compiles
2. Verify all `@PgColumn` fields match intended database columns
3. Verify indexes match query patterns
4. Check nested records have proper `@Builder` and `@With` annotations
</Verification>
