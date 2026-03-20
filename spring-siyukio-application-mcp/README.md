# spring-siyukio-application-mcp

## Module Purpose

This module enables existing APIs to support MCP streamable protocol access, improving development efficiency.

## Key Features

- **Dual Protocol Support**: The same API simultaneously supports both HTTP protocol and MCP protocol with minimal
  configuration changes
- **Automatic Schema Conversion**: Automatically converts OpenAPI schema to MCP schema, significantly reducing code
  duplication
- **Enhanced Interactions**: APIs can support MCP-specific bidirectional communication features such as
  ProgressNotification and Sampling for richer user experiences
- **Flexible Transport Layer**: Adds `WebSocketStreamable` support in addition to official `WebMvcStreamable`, enabling
  direct deployment on Kubernetes clusters without additional MCP session sharing services
- **Virtual Thread Execution**: Automatically switches to virtual threads when called via MCP protocol, enabling
  efficient concurrent execution of long-running tasks
- **Zero-Configuration JWT Support**: Seamlessly integrates with existing JWT authentication for protected MCP tools

## Usage

### Transport Layer

In addition to the official `WebMvcStreamable`, this module adds `WebSocketStreamable` support.

When deploying in a cluster, `WebSocketStreamable` can run directly on the Kubernetes cluster without requiring
additional MCP session sharing services.

### Virtual Thread Execution

When APIs are called via MCP protocol, execution automatically switches to virtual threads, enabling concurrent
execution of a large number of long-running tasks.

### Enabling MCP Support for APIs

Add `mcpTool = true` to `@ApiMapping` to expose the API as an MCP tool.

### Example

```java

@ApiController(tags = "demo")
public class DemoController {

    @ApiMapping(path = "/protected/tool", authorization = true, mcpTool = true,
            summary = "Protected MCP Tool",
            description = "A protected MCP tool that requires JWT authentication."
    )
    public Response protectedTool(Request request, Token token) {
        // implementation
    }
}
```

### MCP Features

#### ProgressNotification

Send progress updates during long-running operations:

```java

@ApiMapping(path = "/long-task", mcpTool = true)
public Response longTask(McpSyncServerExchange exchange) {
    for (int i = 0; i < totalSteps; i++) {
        // Perform work
        doWork(i);

        // Send progress notification
        McpSchema.ProgressNotification progressNotification = new McpSchema.ProgressNotification(
                "",
                0d, 0d,
                String.format("Progress: %d/%d", i + 1, totalSteps)
        );
        exchange.progressNotification(progressNotification);
    }
    return response;
}
```

#### Sampling

Interact with AI models during execution:

```java

@ApiMapping(path = "/ai-task", mcpTool = true)
public Response aiTask(Token token, McpSyncServerExchange exchange) {
    if (exchange != null) {
        // Create sampling request
        McpSchema.CreateMessageRequest request = McpSchema.CreateMessageRequest.builder()
                .maxTokens(100)
                .metadata(metadata)
                .messages(List.of(
                        McpSchema.SamplingMessage.builder()
                                .role("user")
                                .content("Analyze this data")
                                .build()
                ))
                .build();

        McpSchema.CreateMessageResult result = exchange.createMessage(request);
        // Process AI response
    }
    return response;
}
```

---

[← Back to Main Documentation](../README.md)
