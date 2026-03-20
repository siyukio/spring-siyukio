# spring-siyukio-application

## Module Purpose

This module provides a quick way to create HTTP services. It optimizes and integrates on top of Spring Boot:

- `@ApiController` and `@ApiMapping` work together for API definition, efficient and flexible role permission
  validation, CORS, and OpenAPI documentation
- `@ApiParameter` supports complex parameter validation
- `@ApiException` provides unified API exception handling, consistent with MCP protocol
- Integrated OpenAPI, enabling OpenAPI services by environment and displaying API documentation on generic clients
- Integrated spring-boot-actuator

## Key Features

- **Efficient API Definition**: Define REST APIs with `@ApiController` and `@ApiMapping` annotations, eliminating boilerplate code
- **Flexible Security Controls**: Granular security with JWT authorization, request signature validation, and role-based access control - can be used independently or combined
- **Unified Exception Handling**: Consistent error response format aligned with MCP protocol using `@ApiException`
- **Automatic OpenAPI Documentation**: Generate API documentation automatically based on annotations, toggle by environment
- **Built-in CORS Support**: Cross-origin resource sharing configured automatically for API controllers
- **Application Monitoring**: Integrated spring-boot-actuator for health checks, metrics, and runtime management

## Usage

### `@ApiController` and `@ApiMapping`

These annotations are used together to define API endpoints with built-in authorization, CORS, OpenAPI, and other
features.

#### `@ApiController`

- Marks a class as an API controller
- Supports category tags, summary, and role-based access control
- Automatically configures CORS support
- Defines OpenAPI documentation metadata

#### `@ApiMapping`

- Defines API mapping for methods
- Supports role-based access control
- Defines OpenAPI documentation with summary, description, and deprecation info
- Configurable via properties:
    - `authorization`: `true` enables JWT token validation, typically used for user authentication or internal service
      validation
    - `signature`: `true` enables request parameter signature validation to prevent replay attacks. Some public APIs
      without user role authorization can use parameter signature validation alone. Both can be enabled simultaneously
      to increase difficulty for third-party fake clients
    - `roles`: Custom attribute in JWT token. With this, only one set of keys is needed to issue tokens for different
      systems and different types of users
    - `summary`, `description`, `deprecated`: OpenAPI documentation properties

#### Configuration

API role-based authorization validation is based on JWT tokens. JWT tokens include public key, private key, and
password.

- If no keys are configured, HMAC is used
- If keys are configured, RSA and ECDSA are supported
- ES256 is recommended
- Password is used for AES reversible encryption of JWT payload

```yaml
spring:
  siyukio:
    jwt:
      public-key: |
        -----BEGIN PUBLIC KEY-----
        MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEIvOb3WiWoNrQ1ggpeoYz44jxjY+S
        jcIQ4NmtOvfRRYdq4YTTyN4IoMT2mfOO72GfmYYyXhhKP1KonShHvxmBXA==
        -----END PUBLIC KEY-----
      private-key: |
        -----BEGIN PRIVATE KEY-----
        MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAhiWxwhbZlhG5K2f4Z
        hCSvly9hfoK2z2UcpfAyD/n0KQ==
        -----END PRIVATE KEY-----
      password: siyukio
    signature:
      salt: siyukio
```

#### Example

```java

@ApiController(tags = "demo")
public class DemoController {

    // No authorization or signature validation - public API
    @ApiMapping(path = "/public/api", authorization = false, signature = false)
    public Response publicApi(Request request) {
        // implementation
    }

    // Signature validation only - public API with replay attack protection
    @ApiMapping(path = "/public/signed", authorization = false, signature = true)
    public Response publicSignedApi(Request request) {
        // implementation
    }

    // Authorization only - authenticated user or internal service
    @ApiMapping(path = "/protected/api", authorization = true, signature = false)
    public Response protectedApi(Request request) {
        // implementation
    }

    // Both authorization and signature - maximum security
    @ApiMapping(path = "/secure/api", authorization = true, signature = true)
    public Response secureApi(Request request) {
        // implementation
    }

    // Authorization with role validation
    @ApiMapping(path = "/admin/api", authorization = true, roles = {"admin"})
    public Response adminApi(Request request) {
        // implementation
    }
}
```

### `@ApiException`

This exception provides unified API exception handling, consistent with MCP protocol.

#### Error Response Format

When an API exception occurs, the response will be in the following format:

```json
{
  "error": {
    "code": 422,
    "message": "Error description"
  }
}
```

- `code`: The error type that occurred (HTTP status code)
- `message`: A short description of the error

#### Usage

```java

@ApiMapping(path = "/api/create")
public Response createApi(Request request) {
    if (request == null) {
        throw new ApiException("Request cannot be null");
    }
    // implementation
}

// With custom error code
@ApiMapping(path = "/api/update")
public Response updateApi(Request request) {
    if (!isValid(request)) {
        throw new ApiException(400, "Invalid request parameters");
    }
    // implementation
}
```

### OpenAPI

This module integrates OpenAPI for API documentation. The OpenAPI service can be enabled or disabled by environment.

#### Configuration

```yaml
spring:
  siyukio:
    profiles:
      docs: true
```

Set `docs` to `true` to enable OpenAPI service, `false` to disable.

#### Access

When running the service locally at `localhost:8080` and OpenAPI docs is enabled, you can view the API documentation
online at:

```
https://rest.wiki/?http://localhost:8080/api-docs
```

API documentation will be available on generic OpenAPI clients with metadata defined by `@ApiController` and
`@ApiMapping` annotations.

### Spring Boot Actuator

This module integrates spring-boot-actuator for application monitoring and management.

#### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, loggers
```

Configure which endpoints to expose through HTTP. Common endpoints include:
- `health`: Application health status
- `loggers`: Logger configuration and levels
- `metrics`: Application metrics
- `info`: Application information
- `env`: Environment properties

#### API Examples

**Health Check**

```http
GET /actuator/health
```

Response:
```json
{
  "status": "UP"
}
```

**Get Logger Level**

```http
GET /actuator/loggers/io.github.siyukio
```

Response:
```json
{
  "configuredLevel": "DEBUG",
  "effectiveLevel": "DEBUG"
}
```

**Set Logger Level**

```http
POST /actuator/loggers/io.github.siyukio
Content-Type: application/json

{
  "configuredLevel": "TRACE"
}
```

---

[← Back to Main Documentation](../README.md)