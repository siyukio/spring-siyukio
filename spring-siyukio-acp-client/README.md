# spring-siyukio-acp-client

## Module Purpose

This module provides a lightweight, easy-to-use ACP (Agent Client Protocol) client implementation for connecting to ACP
servers. It enables Java applications to act as ACP clients and communicate with AI agent services through WebSocket
transport.

## Key Features

- **Simple API** — High-level, intuitive API for ACP operations with minimal boilerplate code
- **WebSocket Transport** — Built-in WebSocket client transport for real-time bidirectional communication
- **Tool Call Support** — Execute remote tool calls with automatic parameter serialization and response deserialization
- **Session Management** — Create, load, and manage ACP sessions with proper lifecycle handling
- **Client-Side Capabilities** — Handle server requests for file operations, terminal commands, and permission prompts
- **Asynchronous Foundation** — Built on reactive programming model with Project Reactor support
- **Timeout Management** — Configurable timeouts for all operations with proper error handling

## Quick Start

### 1. Add Dependency

```xml

<dependency>
    <groupId>io.github.siyukio</groupId>
    <artifactId>spring-siyukio-acp-client</artifactId>
    <version>${spring-siyukio.version}</version>
</dependency>
```

### 2. Create ACP Client Instance

```java
import io.github.siyukio.client.SimpleAcpClient;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;

// Create token for authentication
Token token = Token.builder()
        .uid("user123")
        .name("Test User")
        .roles(List.of("user"))
        .build();

        String authorization = tokenProvider.createAuthorization(token);
        String serverUri = "ws://localhost:8080";

        // Build ACP client
        SimpleAcpClient acpClient = SimpleAcpClient.builder(serverUri)
                .requestTimeout(Duration.ofSeconds(60))
                .authorization(authorization)
                .build();

        // Create new session
        AcpSchema.NewSessionResponse sessionResponse = acpClient.newSession();
        String sessionId = sessionResponse.sessionId();
```

### 3. Execute Tool Calls

```java
// Call remote tool with parameters
CreateAuthorizationRequest request = CreateAuthorizationRequest.builder()
                .uid("test").name("test").roles(List.of()).build();

CreateAuthorizationResponse response = acpClient.callTool(
        "authorization.create",
        request,
        CreateAuthorizationResponse.class);

// Call tool without parameters
TokenResponse tokenResponse = acpClient.callTool(
        "toolCallProgress",
        TokenResponse.class);

// List available tools
AcpSchemaExt.ListToolsResult tools = acpClient.listTools();
```

### 4. Handle Client-Side Capabilities

```java
SimpleAcpClient acpClient = SimpleAcpClient.builder(serverUri)
        .authorization(authorization)

        // Handle file read requests from server
        .readTextFileHandler(request -> {
            log.debug("ReadTextFileRequest: {}", request);
            // Read file from local filesystem
            String content = Files.readString(Path.of(request.path()));
            return new AcpSchema.ReadTextFileResponse(content);
        })

        // Handle file write requests from server
        .writeTextFileHandler(request -> {
            log.debug("WriteTextFileRequest: {}", request);
            // Write file to local filesystem
            Files.writeString(Path.of(request.path()), request.contents());
            return new AcpSchema.WriteTextFileResponse();
        })

        // Handle terminal operations
        .terminalHandler(new SimpleAcpClient.TerminalHandler() {
            @Override
            public SimpleAcpClient.CreateTerminalHandler createTerminalHandler() {
                return request -> new AcpSchema.CreateTerminalResponse(IdUtils.getUniqueId());
            }

            @Override
            public SimpleAcpClient.WaitForTerminalExitHandler waitForTerminalExitHandler() {
                return request -> new AcpSchema.WaitForTerminalExitResponse(0, null);
            }

            @Override
            public SimpleAcpClient.TerminalOutputHandler terminalOutputHandler() {
                return request -> new AcpSchema.TerminalOutputResponse(
                        "System information", true,
                        new AcpSchema.TerminalExitStatus(0, null));
            }

            @Override
            public SimpleAcpClient.ReleaseTerminalHandler releaseTerminalHandler() {
                return request -> new AcpSchema.ReleaseTerminalResponse();
            }
        })

        // Handle permission requests from server
        .requestPermissionHandler(request -> {
            log.debug("RequestPermission: {}", request);
            // Show permission dialog to user
            boolean granted = showPermissionDialog(request.description());
            return new AcpSchema.RequestPermissionResponse(
                    new AcpSchema.PermissionGranted(granted));
        })

        // Handle progress notifications
        .progressNotificationHandler(notification -> {
            log.debug("ProgressNotification: {}", notification);
            updateProgressBar(notification.progress(), notification.total());
        })

        .build();
```