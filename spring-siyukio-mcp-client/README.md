# spring-siyukio-mcp-client

## Module Purpose

This module provides `McpSyncClient` for accessing APIs through MCP streamable protocol.

## Key Features

- **Virtual Thread Support**: Automatically uses virtual threads, allowing large numbers of long-running blocking
  requests without exhausting server threads
- **WebSocket Transport**: Enable `WebSocketStreamable` transport by calling `useWebsocket(true)` for better performance
  and clustering support
- **Flexible Authentication**: Support both static tokens and dynamic token suppliers via `useTokenSupplier()`
- **Progress Notifications**: Built-in support for handling progress updates from long-running operations
- **AI Sampling Handler**: Customizable handler for AI model sampling requests
- **Spring Integration**: Seamless integration with Spring Boot configuration properties
- **Rich Builder API**: Fluent builder pattern with comprehensive configuration options including timeout, custom
  headers, and endpoint configuration

## Usage

### Creating MCP Client

Use `McpSyncClient.builder()` to create an MCP client with desired configuration.

#### Configuration

```yaml
spring:
  siyukio:
    mcp-client:
      base-url: http://localhost:8080
      mcp-endpoint: /mcp
      name: mcp-client
      version: 0.16.0
      request-timeout: 60s
```

#### Basic Example

```java
McpSyncClient client = McpSyncClient.builder()
        .setBaseUrl("http://localhost:8080")
        .setMcpEndpoint("/mcp")
        .build();

// List available tools
McpSchema.ListToolsResult tools = client.listTools();

// Call a tool without parameters
JSONObject result = client.callTool("toolName", JSONObject.class);

// Call a tool with parameters
CreateAuthRequest request = CreateAuthRequest.builder()
        .uid("123")
        .name("user")
        .build();
JSONObject result = client.callTool("authorization.create", request, JSONObject.class);
```

### WebSocket Transport

Enable WebSocket transport for better performance and clustering:

```java
McpSyncClient client = McpSyncClient.builder()
        .useWebsocket(true)  // Enable WebSocketStreamable
        .setBaseUrl("http://localhost:8080")
        .build();
```

### Authentication

#### Static Token

```java
McpSyncClient client = McpSyncClient.builder()
        .setAuthorization("Bearer your-token")
        .build();
```

### Progress Notifications

Handle progress updates from long-running operations:

```java
Consumer<McpSchema.ProgressNotification> progressHandler = notification -> {
    log.info("Progress: {}", notification.message());
};

McpSyncClient client = McpSyncClient.builder()
        .setProgressHandler(progressHandler)
        .build();
```

### Sampling Handler

Handle AI model sampling requests:

```java
Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler = request -> {
    // Implement AI model interaction
    log.info("Sampling request: {}", request);
    return McpSchema.CreateMessageResult.builder()
            .role(McpSchema.Role.USER)
            .message("AI response")
            .build();
};

McpSyncClient client = McpSyncClient.builder()
        .setSamplingHandler(samplingHandler)
        .build();
```

### Builder Options

| Method                                     | Description                   | Default            |
|--------------------------------------------|-------------------------------|--------------------|
| `setBaseUrl(String)`                       | Base URL of MCP server        | `http://localhost` |
| `setMcpEndpoint(String)`                   | MCP endpoint path             | `/mcp`             |
| `useWebsocket(boolean)`                    | Enable WebSocket transport    | `false`            |
| `setAuthorization(String)`                 | Static authorization token    | empty              |
| `useTokenSupplier(Supplier<String>)`       | Dynamic token provider        | null               |
| `setRequestTimeout(Duration)`              | Request timeout               | `60s`              |
| `setName(String)`                          | Client name                   | `mcp-client`       |
| `setVersion(String)`                       | Client version                | `0.16.0`           |
| `addHeader(String, String)`                | Add custom header             | -                  |
| `setProgressHandler(Consumer)`             | Progress notification handler | null               |
| `setSamplingHandler(Function)`             | AI sampling handler           | null               |
| `setMcpClientCommonProperties(Properties)` | Load from Spring properties   | -                  |

---

[← Back to Main Documentation](../README.md)
