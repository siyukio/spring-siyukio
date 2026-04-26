---
name: siyukio-create-api
description: "Generate API layer (Controller + DTOs) for Siyukio-based Spring Boot applications using @ApiController, @ApiMapping, @ApiParameter annotations"
triggers:
  - "add api"
  - "create api"
  - "new api"
  - "add endpoint"
  - "new endpoint"
  - "api add"
  - "api create"
---

<Purpose>
Generate API layer code for Siyukio Spring Boot applications. This is a fundamental programming skill that exposes Application Services to clients via HTTP or ACP protocols. All requests are automatically validated for user token, parameters, and signature.

API structure under domain module:

```
api/
├── {Context}Controller.java
├── paths/
│   └── {Context}Paths.java      (interface with path constants)
└── dto/
    ├── {Context}Request.java    (record)
    └── {Context}Response.java   (record)
```

Each `{Context}Controller` uses a corresponding `{Context}Paths` interface that defines all path constants. This ensures
type safety and centralizes path management.

</Purpose>

<Use_When>

- Creating new API endpoints
- Exposing Application Service methods to clients
- Defining request/response DTOs with parameter validation
- Implementing APIs with token/signature verification
  </Use_When>

<Prerequisites>
- Target domain module must exist: `{project-name}/{project-name}-domain-{domain}/`
- Module must have `spring-siyukio-application-acp` dependency in pom.xml
- Target file location: `src/main/java/{package-path}/{domain}/api/`
- JWT configuration in `application.yml` for token authorization (see below)
</Prerequisites>

### Configuration Notes

- **JWT**: Configure `spring.siyukio.jwt.*` for token authorization (see `{project-name}` conventions)
- **Signature**: Configure `spring.siyukio.signature.salt` when `signature = true` in `@ApiMapping`

<Execution_Protocol>

## Step 1: Determine API Requirements

From the argument, extract:

- `{Domain}`: Domain module name (kebab-case, e.g., `user-management`)
- `{Context}`: The business context name (PascalCase, e.g., `User`)
- Operation type: `create`, `get`, `update`, `list`
- Request fields: name, type, validation rules
- Response fields: name, type
- ACP support: Set `acpAvailable = true` if ACP protocol access is needed

> **Note**: All APIs use POST with JSON body. Path constants are defined in `{Context}Paths` interface.

## Step 2: Verify/Add Maven Dependency

Ensure the domain module `pom.xml` has:

```xml
<dependency>
    <groupId>io.github.siyukio</groupId>
    <artifactId>spring-siyukio-application-acp</artifactId>
</dependency>
```

## Step 3: Generate Request DTO

Location:
`{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/api/dto/{Context}Request.java`

**Must use Java record.** Every field must have `@ApiParameter` annotation - fields without it are automatically
filtered.

```java
package {package-name}.{domain}.api.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.validation.Validated;
import jakarta.validation.constraints.*;

/**
 * Request DTO for {Context} operations.
 */
public record {Context}Request(
        @ApiParameter(description = "ID", required = true)
        String id,

        @ApiParameter(description = "Name", required = true)
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @ApiParameter(description = "Optional description")
        String description
) implements Validated {
}
```

## Step 4: Generate Response DTO

Location:
`{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/api/dto/{Context}Response.java`

**Must use Java record.** Every field must have `@ApiParameter` annotation - fields without it are automatically
filtered.

```java
package {package-name}.{domain}.api.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import java.time.LocalDateTime;

/**
 * Response DTO for {Context} operations.
 */
public record {Context}Response(
        @ApiParameter(description = "Unique identifier")
        String id,

        @ApiParameter(description = "Name")
        String name,

        @ApiParameter(description = "Description")
        String description,

        @ApiParameter(description = "Creation timestamp")
        LocalDateTime createdAt,

        @ApiParameter(description = "Last update timestamp")
        LocalDateTime updatedAt
) {
}
```

## Step 5: Generate Paths Interface

Location:
`{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/api/paths/{Context}Paths.java`

**Use interface to define all path constants. Path naming rules:**

- Simple (single context): `/{domain}/create`, `/{domain}/get`, etc.
- Complex (multiple contexts): `/{domain}/create{Context}`, `/{domain}/get{Context}`, etc.

```java
package {package-name}.{domain}.api.paths;

/**
 * Path constants for {Context} API endpoints.
 */
public interface {Context}Paths {

    String LIST = "/{domain}/list{Context}";
    String CREATE = "/{domain}/create{Context}";
    String GET = "/{domain}/get{Context}";
    String UPDATE = "/{domain}/update{Context}";
    String REMOVE = "/{domain}/remove{Context}";
}
```

## Step 6: Generate API Controller

Location:
`{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/api/{Context}Controller.java`

```java
package {package-name}.{domain}.api;

import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.dto.PageRequest;
import io.github.siyukio.tools.api.dto.PageResponse;
import io.github.siyukio.tools.api.token.Token;
import {package-name}.{domain}.api.dto.{Context}Request;
import {package-name}.{domain}.api.dto.{Context}Response;
import {package-name}.{domain}.api.paths.{Context}Paths;
import {package-name}.{domain}.application.{Context}Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@ApiController(summary = "{Context} API")
public class {Context}Controller {

    @Autowired
    private {Context}Service {context}Service;

    @ApiMapping(path = {Context}Paths.GET, summary = "Get {Context} by ID")
    public {Context}Response get(Token token, @ApiParameter(description = "Request") {Context}Request request) {
        log.debug("User: {}", token.uid());
        return {context}Service.getById(request.id());
    }

    @ApiMapping(path = {Context}Paths.CREATE, summary = "Create new {Context}")
    public {Context}Response create(Token token, @ApiParameter(description = "Request") {Context}Request request) {
        return {context}Service.create(request);
    }

    @ApiMapping(path = {Context}Paths.UPDATE, summary = "Update existing {Context}")
    public {Context}Response update(Token token, @ApiParameter(description = "Request") {Context}Request request) {
        return {context}Service.update(request);
    }

    @ApiMapping(path = {Context}Paths.LIST, summary = "Query {Context} list with pagination")
    public PageResponse<{Context}Response> list(Token token, @ApiParameter(description = "Page request") PageRequest request) {
        return {context}Service.queryPage(request);
    }
}
```

> **Note**: Add `Token token` parameter to access authenticated user information. Authorization is enabled by default (
`authorization = true`). Failed authentication will be intercepted and return an error response.

</Execution_Protocol>

<Key_Conventions>

| Item         | Convention                                                               |
|--------------|--------------------------------------------------------------------------|
| Package      | `{package-name}.{domain}.api`                                            |
| Package Path | `{package-path}/{domain}/api/`                                           |
| Paths        | `{Context}Paths.java` interface with path constants in `api/paths/`      |
| Controller   | `{Context}Controller.java` with `@ApiController`                         |
| Request DTO  | `{Context}Request.java` **must use record** implements `Validated`       |
| Response DTO | `{Context}Response.java` **must use record**                             |
| DTO Fields   | **Every field must have @ApiParameter** - fields without it are filtered |
| Pagination   | Use `PageRequest` and `PageResponse` from `io.github.siyukio.tools.api`  |
| Path Pattern | `/{domain}/{operation}` or `/{domain}/{operation}{Context}`              |
| Operation    | `create`, `get`, `update`, `list`, `remove`                              |
| Request Type | All APIs use POST with JSON body                                         |
| ACP Support  | Set `acpAvailable = true` in `@ApiMapping` to enable ACP protocol        |

</Key_Conventions>

<Annotation_Reference>

### @ApiController

Defines a group of APIs for a business context.

```java
@ApiController(tags = {"{Context}"}, summary = "{Context} API")
public class {Context}Controller {
}
```

| Property  | Default | Description                                              |
|-----------|---------|----------------------------------------------------------|
| `tags`    | {}      | **Recommended.** Category tags for filtering in api-docs |
| `summary` | ""      | **Recommended.** Brief description of the controller     |

> Use `tags` and `summary` together to quickly filter APIs in api-docs.

### @ApiMapping

Defines a single API endpoint. Use path constants from `{Context}Paths` interface.

```java
@ApiMapping(path = {Context}Paths.GET, summary = "Get {Context} by ID")
public {Context}Response get({Context}Request request) {}

@ApiMapping(path = {Context}Paths.CREATE, summary = "Create new")
public {Context}Response create({Context}Request request) {}

@ApiMapping(path = {Context}Paths.UPDATE, summary = "Update existing")
public {Context}Response update({Context}Request request) {}

@ApiMapping(path = {Context}Paths.LIST, summary = "Query list")
public PageResponse<{Context}Response> list(PageRequest request) {}
```

| Property        | Default | Description                                                           |
|-----------------|---------|-----------------------------------------------------------------------|
| `path`          | -       | **Required.** Use `{Context}Paths.GET`, `{Context}Paths.CREATE`, etc. |
| `summary`       | ""      | **Recommended.** Brief description of the API                         |
| `description`   | ""      | Detailed description of the API                                       |
| `authorization` | true    | Whether the API requires authorization validation                     |
| `signature`     | false   | Whether the API requires parameter signature validation               |
| `roles`         | {}      | Required roles for authorization                                      |
| `deprecated`    | false   | Whether the API is deprecated                                         |
| `acpAvailable`  | false   | Enable ACP protocol access                                            |

### @ApiParameter

Defines request or response parameters with validation. **Every DTO field must have this annotation** - fields without
it are automatically filtered from input/output.

Import: `io.github.siyukio.tools.api.annotation.ApiParameter`

```java
// In Request DTO - every field requires @ApiParameter
public record {Context}Request(
        @ApiParameter(description = "ID")
        String id,

        @ApiParameter(description = "Name", required = true)
        String name,

        @ApiParameter(description = "Optional description")
        String description
) implements Validated {
}
```

| Property      | Default | Description                                                                         |
|---------------|---------|-------------------------------------------------------------------------------------|
| `required`    | true    | Whether it is required                                                              |
| `description` | ""      | Parameter description (defaults to field name, only set when field name is unclear) |

> **Note**: For Response DTOs, only configure `description` when the field name itself is unclear. Validation properties
> only apply to Request DTOs.

### Common Pagination Types

Use built-in `PageRequest` and `PageResponse` from `io.github.siyukio.tools.api.dto` instead of custom pagination.

### Token Response

```java
import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.response.TokenResponse;

// TokenResponse contains user authentication info
public record TokenResponse(
        @ApiParameter
        String uid,

        @ApiParameter
        String name,

        @ApiParameter
        List<String> roles
) {
}
```

### Automatic Validations

All API requests are automatically validated:

1. **User Token Validation**: Verify user authentication token
2. **Parameter Validation**: Jakarta Bean Validation on DTO fields
3. **Signature Validation**: Request signature verification

</Annotation_Reference>

<Verification>
After implementation:
1. Run `./mvnw compile` to verify code compiles
2. Check all imports are correct
3. Verify DTO field mappings match Application Service
4. Test POST JSON requests with correct operation paths
5. If ACP enabled, verify ACP client configuration
</Verification>

<Unit_Testing>

## Step 1: Analyze API for Test Generation

Based on the API's description, request DTO, and response DTO, identify:

- Test scenarios (positive, negative, edge cases)
- Required test data
- Assertions for response validation
- Dependencies on other APIs

## Step 2: Generate Spring Boot Test Entry (if not exists)

Location: `{project-name}/{project-name}-domain-{domain}/src/test/java/{package-path}/Test{Domain}Application.java`

One entry per module. Only create if not exists.

```java
package {package-name};

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Test{Domain}Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Test{Domain}Application.class)
                .build()
                .run(args);
    }

}
```

## Step 3: Prepare Local Test Configuration

Extract local test environment variables from context and create:

Location: `{project-name}/{project-name}-domain-{domain}/src/test/resources/application-local.yml`

```yaml
spring:
  datasource:
    postgres:
      master:
        url: ${SIYUKIO_DB_MASTER_URL}
        username: ${SIYUKIO_DB_MASTER_USERNAME}
        password: ${SIYUKIO_DB_MASTER_PASSWORD}
```

## Step 4: Generate Unit Test Class

Location: `{project-name}/{project-name}-domain-{domain}/src/test/java/{package-path}/{domain}/api/{Context}ControllerTest.java`

```java
package {package-name}.{domain}.api;

import {package-name}.{domain}.api.dto.{Context}Request;
import {package-name}.{domain}.api.dto.{Context}Response;
import {package-name}.{domain}.api.paths.{Context}Paths;
import io.github.siyukio.tools.test.api.ApiMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class {Context}ControllerTest {

    @Autowired
    private ApiMock apiMock;

    @Test
    void testCreate{Context}() {
        {Context}Response response = this.apiMock.perform(
                {Context}Paths.CREATE,
                new {Context}Request(null, "Test Name", "Description"),
                {Context}Response.class);

        assertNotNull(response.id());
        assertEquals("Test Name", response.name());
    }

    @Test
    void testGet{Context}() {
        // Create test data first
        {Context}Response created = this.apiMock.perform(
                {Context}Paths.CREATE,
                new {Context}Request(null, "Test Name", "Description"),
                {Context}Response.class);

        // Test GET endpoint
        {Context}Response response = this.apiMock.perform(
                {Context}Paths.GET,
                new {Context}Request(created.id(), null, null),
                {Context}Response.class);

        assertEquals(created.id(), response.id());
        assertEquals("Test Name", response.name());
    }
}
```

## Step 5: Execute Tests

Run tests:

```bash
./mvnw test -DskipTests=false -pl {project-name}/{project-name}-domain-{domain}
```

</Unit_Testing>
