# spring-siyukio-http-client

## Module Purpose

This module provides `@ApiClient` for accessing Siyukio APIs between services.

## Key Features

- **Transparent Exception Handling**: Seamlessly propagates `ApiException` across service boundaries
- **Virtual Thread Support**: Automatically leverages virtual threads for efficient, lightweight concurrency
- **Resource-Optimized**: Reuses JDK HttpClient instances per protocol, preventing thread pool exhaustion
- **Spring Integration**: Fully compatible with `@HttpExchange`, serving as both a specialized and general-purpose HTTP
  client

## Usage

### `@ApiClient`

Use `@ApiClient` annotation to create a typed HTTP client interface.

#### Example

```java

@ApiClient(url = "${spring.siyukio.api-docs.url}", headers = {
        "Authorization=${spring.siyukio.api-docs.authorization}"
})
public interface DemoApiClient {

    @GetExchange("/actuator/health")
    Health health();

    @PostExchange("/authorization/create")
    JSONObject createAuthorization(@RequestBody CreateAuthorizationRequest request);

    @GetExchange("/users/{id}")
    User getUser(@PathVariable String id);

    @PostExchange("/users")
    User createUser(@RequestBody UserRequest request);

    record Health(String status) {
    }
}
```

---

[← Back to Main Documentation](../README.md)