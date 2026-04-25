---
name: siyukio-create-application
description: "Generate Application Service layer for Siyukio-based Spring Boot applications, exposing domain functionality to API layer"
triggers:
  - "add application service"
  - "create application service"
  - "new application service"
  - "application service add"
  - "application service create"
---

<Purpose>
Generate Application Service layer for Siyukio Spring Boot applications. The Application Service acts as a bridge between the API layer and the domain layer, coordinating domain operations and transactions.

Application layer structure under domain module:

```
{domain}/
└── application/
    └── {Context}Service.java
```

</Purpose>

<Use_When>

- Creating a new Application Service
- Implementing business logic coordination
- Exposing domain functionality to API layer
- Implementing CRUD operations with domain entities

</Use_When>

<Prerequisites>

**Important workflow order:**

1. If domain model/policy needs to be created or updated: use `$siyukio-create-domain` first
2. If API/controller/DTO needs to be created or updated: use `$siyukio-create-api` first
3. Finally, create the Application Service

Requirements:

- Target domain module: `{project-name}/{project-name}-domain-{domain}/`
- Domain model must exist: `src/main/java/{package-path}/{domain}/domain/model/{Entity}.java`
- Policy may exist: `src/main/java/{package-path}/{domain}/domain/policy/{Entity}Policy.java`
- Target file location: `src/main/java/{package-path}/{domain}/application/`

</Prerequisites>

<Execution_Protocol>

## Step 1: Determine Application Service Requirements

From the argument, extract:

- `{Domain}`: Domain module name (kebab-case, e.g., `user-management`)
- `{Context}`: The business context name (PascalCase, e.g., `Skill`)
- `{entity}`: The entity variable name (camelCase, e.g., `skill`)
- Operations: which operations are needed (get, create, update, list, delete)
- Policy methods: check existence, check uniqueness, etc.

## Step 2: Verify/Setup Policy

When implementing methods that require querying and validating Entity, follow this workflow:

1. **Check existing Policy**: Look for `{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/domain/policy/{Domain}Policy.java`

2. **If Policy does not exist**: Create it first using `$siyukio-create-domain` skill with "add policy" trigger

3. **If Policy exists but lacks required method**: Add the validation method to the existing Policy

4. **Common Policy methods needed**:
   - `check{Entity}Exists(id)` - Query by ID, throw exception if not found
   - `check{Entity}Enabled(id)` - Query by ID, throw exception if not found or disabled
   - `check{Entity}NameUnique(...)` - Validate uniqueness of name field

5. **Service never directly uses PgEntityDao for validation**: Always delegate to Policy

## Step 3: Generate Application Service

Location: `{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/application/{Context}Service.java`

**Service design rules:**

- Service method signatures must match API controller (same method names, request params, response types)
- Inject `PgEntityDao<Entity>` for direct database operations
- Inject `{Context}Policy` for validation and business rule checks
- Use `Token` parameter if user context is needed
- Use `XDataUtils` for DTO ↔ Entity conversions
- Add `@Transactional` annotation on methods that involve multiple record operations (insert, update, delete)

```java
package {package-name}.{domain}.application;

import {package-name}.{domain}.api.dto.*;
import {package-name}.{domain}.domain.model.{Entity};
import {package-name}.{domain}.domain.policy.{Entity}Policy;
import io.github.siyukio.tools.api.dto.PageRequest;
import io.github.siyukio.tools.api.dto.PageResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.query.BoolQueryBuilder;
import io.github.siyukio.tools.entity.query.QueryBuilders;
import io.github.siyukio.tools.entity.sort.FieldSortBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilders;
import io.github.siyukio.tools.entity.sort.SortOrder;
import io.github.siyukio.tools.util.XDataUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for {Context} operations.
 */
@Service
public class {Context}Service {

    @Autowired
    private PgEntityDao<{Entity}> {entity}PgEntityDao;

    @Autowired
    private {Entity}Policy {entity}Policy;

    /**
     * Query {Context} list with pagination.
     */
    public PageResponse<{Context}Item> query{Context}s(PageRequest<{Context}Query> pageRequest) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        {Context}Query query = pageRequest.filter();

        if (query != null) {
            if (query.sourceId() != null) {
                boolQueryBuilder.must(QueryBuilders.termQuery("sourceId", query.sourceId()));
            }
            if (query.name() != null) {
                boolQueryBuilder.must(QueryBuilders.termQuery("name", query.name()));
            }
            if (query.enabled() != null) {
                boolQueryBuilder.must(QueryBuilders.termQuery("enabled", query.enabled()));
            }
        }

        // Sort: enabled DESC, updatedAtTs DESC
        FieldSortBuilder enabledBuilder = SortBuilders.fieldSort("enabled").order(SortOrder.DESC);
        FieldSortBuilder updateTimeBuilder = SortBuilders.fieldSort("updatedAtTs").order(SortOrder.DESC);

        Page<{Entity}> page = this.{entity}PgEntityDao.queryPage(
                boolQueryBuilder,
                SortBuilders.fieldSort(enabledBuilder, updateTimeBuilder),
                pageRequest.page(),
                pageRequest.size()
        );

        // Use XDataUtils for deep copy conversion
        List<{Context}Item> items = XDataUtils.copy(page.items(), List.class, {Context}Item.class);
        return PageResponse.<{Context}Item>builder().total(page.total()).items(items).build();
    }

    /**
     * Get {Context} by ID.
     */
    public {Context}Response get{Context}(Get{Context}Request request) {
        {Entity} {entity} = this.{entity}Policy.check{Entity}Exists(request.id());
        return XDataUtils.copy({entity}, {Context}Response.class);
    }

    /**
     * Create a new {Context}.
     */
    public {Context}Response create{Context}(Create{Context}Request request) {
        // Validate business rules via Policy
        {entity}Policy.check{Entity}NameUnique(request.sourceId(), request.name(), null);

        {Entity} {entity} = {Entity}.builder()
                .sourceId(request.sourceId())
                .name(request.name())
                .description(request.description())
                .enabled(request.enabled() != null ? request.enabled() : true)
                .build();

        {entity} = this.{entity}PgEntityDao.insert({entity});
        return XDataUtils.copy({entity}, {Context}Response.class);
    }

    /**
     * Update an existing {Context}.
     */
    public {Context}Response update{Context}(Update{Context}Request request) {
        {Entity} {entity} = this.{entity}Policy.check{Entity}Exists(request.id());

        // Check uniqueness if name or sourceId is being changed
        if (request.sourceId() != null && request.name() != null &&
                (!request.sourceId().equals({entity}.sourceId()) ||
                        !request.name().equals({entity}.name()))) {
            {entity}Policy.check{Entity}NameUnique(request.sourceId(), request.name(), {entity}.id());
        }

        // Merge non-null fields from request to entity
        {entity} = XDataUtils.mergeNotNul(request, {entity});

        {entity} = this.{entity}PgEntityDao.update({entity});
        return XDataUtils.copy({entity}, {Context}Response.class);
    }

    /**
     * Delete {Context} by ID.
     */
    public void delete{Context}(Delete{Context}Request request) {
        {Entity} {entity} = this.{entity}Policy.check{Entity}Exists(request.id());
        this.{entity}PgEntityDao.delete({entity});
    }
}
```

</Execution_Protocol>

<Key_Conventions>

| Item             | Convention                                                                        |
| ---------------- | --------------------------------------------------------------------------------- |
| Package          | `{package-name}.{domain}.application`                                             |
| Package Path     | `{package-path}/{domain}/application/`                                            |
| Service class    | `{Context}Service.java` with `@Service`                                           |
| DAO injection    | `@Autowired private PgEntityDao<Entity> {entity}PgEntityDao` (only for batch queries) |
| Policy injection | `@Autowired private {Domain}Policy {domain}Policy`                                |
| Validation       | Always use Policy for Entity validation, never query directly via DAO             |
| DTO conversion   | Use `XDataUtils.copy(source, TargetClass.class)` for Entity → DTO                 |
| DTO merge        | Use `XDataUtils.mergeNotNul(source, target)` for DTO → Entity update              |
| User context     | Add `Token token` parameter if user information is needed                         |
| Pagination       | Use `PageRequest<T>` and `PageResponse<T>` from `io.github.siyukio.tools.api.dto` |
| Query building   | Use `BoolQueryBuilder` + `QueryBuilders.termQuery()` for complex conditions      |
| Sorting          | Use `SortBuilders.fieldSort()` with `SortOrder.DESC/ASC`                         |
| Transaction      | Add `@Transactional` on methods with multiple insert/update/delete operations     |

</Key_Conventions>

<SortBuilders_Usage>

## SortBuilders (排序构建器)

| Method                                      | Purpose                        |
| ------------------------------------------- | ------------------------------ |
| `SortBuilders.fieldSort(field)`             | Create field sort             |
| `SortBuilders.fieldSort(field).order(order)` | Set sort order (ASC/DESC)     |
| `SortBuilders.fieldSort(builder1, builder2)` | Multi-field sort              |

## SortOrder (排序方向)

| Value  | Description |
| ------ | ----------- |
| `ASC`  | Ascending   |
| `DESC` | Descending  |

> **Import**: `io.github.siyukio.tools.entity.sort.SortBuilders`, `io.github.siyukio.tools.entity.sort.SortOrder`

</SortBuilders_Usage>

<QueryBuilders_Usage>

## QueryBuilders (查询构建器)

| Method                                           | Purpose                            |
| ------------------------------------------------ | ---------------------------------- |
| `QueryBuilders.boolQuery()`                      | Create a boolean query container   |
| `QueryBuilders.termQuery(name, value)`           | Exact match query (term)           |
| `QueryBuilders.termsQuery(name)`                 | Terms query builder for IN queries |
| `QueryBuilders.rangeQuery(name)`                 | Range query (gt, gte, lt, lte, eq) |
| `QueryBuilders.matchQuery(name, value)`          | Full-text match query              |
| `QueryBuilders.wildcardQuery(name, pattern)`     | Wildcard query (\*, ?)             |
| `QueryBuilders.wildcardPrefixQuery(name, value)` | Prefix wildcard (\*value)         |
| `QueryBuilders.wildcardSuffixQuery(name, value)` | Suffix wildcard (value\*)         |

## BoolQueryBuilder (布尔查询)

| Method                | Purpose       |
| --------------------- | ------------- |
| `bool.must(query)`    | AND condition |
| `bool.mustNot(query)` | NOT condition |
| `bool.should(query)`  | OR condition  |

> **Import**: `io.github.siyukio.tools.entity.query.QueryBuilders`

</QueryBuilders_Usage>

<XDataUtils_Usage>

## Object Copy (深度复制)

| Method                                               | Purpose                                   |
| ---------------------------------------------------- | ----------------------------------------- |
| `XDataUtils.copy(from, TargetClass.class)`            | Copy object to target type (Entity → DTO) |
| `XDataUtils.copy(from, List.class, Item.class)`       | Copy List with type transformation        |
| `XDataUtils.copy(from, Map.class, K.class, V.class)`  | Copy Map with key/value types             |

## Object Merge (合并非null属性)

| Method                                   | Purpose                                     |
| ---------------------------------------- | ------------------------------------------- |
| `XDataUtils.mergeNotNul(source, target)` | Merge non-null fields from source to target |

## JSON Parse (JSON解析)

| Method                          | Purpose                         |
| ------------------------------- | ------------------------------- |
| `XDataUtils.parse(json, Class)` | Parse JSON string to object     |
| `XDataUtils.parseObject(json)`  | Parse JSON string to JSONObject |
| `XDataUtils.parseArray(json)`   | Parse JSON string to JSONArray  |

## JSON Serialize (JSON序列化)

| Method                                | Purpose                          |
| ------------------------------------- | -------------------------------- |
| `XDataUtils.toJSONString(from)`       | Convert object to JSON string    |
| `XDataUtils.toPrettyJSONString(from)` | Convert to formatted JSON string |

## DateTime (日期时间)

| Method                             | Purpose                                                     |
| ---------------------------------- | ---------------------------------------------------------- |
| `XDataUtils.parse(text)`           | Parse string to LocalDateTime (multiple formats supported) |
| `XDataUtils.format(localDateTime)` | Format LocalDateTime to string                             |
| `XDataUtils.formatMs(timestamp)`   | Format timestamp to formatted string                       |

> **Import**: `io.github.siyukio.tools.util.XDataUtils`

</XDataUtils_Usage>

<Verification>
After implementation:
1. Run `./mvnw compile` to verify code compiles
2. Check all imports are correct
3. Verify service methods match API controller method signatures
4. Verify DTO field mappings match entity
</Verification>
