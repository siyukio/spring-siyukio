# spring-siyukio-application-acp

## Module Purpose

This module provides Agent Client Protocol (ACP) server implementation for building AI agent services. It enables
bidirectional communication between Spring Boot applications and ACP clients through WebSocket transport.

## Key Features

- **Zero-Configuration ACP Server** — Auto-configure WebSocket ACP transport with Spring Boot auto-configuration
- **Session Management** — Built-in session lifecycle handling via `AcpSessionHandler`
- **Bidirectional Communication** — Real-time interaction with ACP clients through `AcpSessionContext`
- **Progress Notifications** — Send progress updates during long-running operations
- **Permission & Choice Dialogs** — Interactive user prompts for permission requests and choice selection
- **Command Execution** — Execute system commands and file operations with client-side capabilities

## Architecture Overview

```
┌─────────────────┐    WebSocket    ┌─────────────────┐
│   ACP Client    │◄───────────────►│  Spring Boot    │
│                 │                 │  Application    │
│  (e.g., Claude) │                 │                 │
└─────────────────┘                 └─────────────────┘
                                             │
                                    ┌────────▼────────┐
                                    │   AcpSession    │
                                    │   Handler       │
                                    └─────────────────┘
```

## Quick Start

### 1. Add Dependency

```xml

<dependency>
    <groupId>io.github.siyukio</groupId>
    <artifactId>spring-siyukio-application-acp</artifactId>
    <version>${spring-siyukio.version}</version>
</dependency>
```

### 2. Implement AcpSessionHandler

Create a Spring bean implementing `AcpSessionHandler` interface to handle ACP session events:

```java

@Service
public class MyAcpSessionHandler implements AcpSessionHandler {
    @Override
    public AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        log.debug("AcpSchema.NewSessionRequest: {}, {}", token, req);
        AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
        AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
        return new AcpSchema.NewSessionResponse(token.id(), sessionModeState, sessionModelState);
    }

    @Override
    public AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        log.debug("AcpSchema.LoadSessionResponse: {}, {}", token, req);
        if (req.sessionId().equals(token.id())) {
            AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
            AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
            return new AcpSchema.LoadSessionResponse(sessionModeState, sessionModelState);
        }
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Session not found:" + req.sessionId());
    }

    @Override
    public void handleCancel(Token token, AcpSchema.CancelNotification req) {
        log.debug("AcpSchema.CancelNotification: {}, {}", token, req);
    }

    @Override
    public AcpSchema.SetSessionModeResponse handleSetSessionMode(Token token, AcpSchema.SetSessionModeRequest req) {
        log.debug("AcpSchema.SetSessionModeRequest: {}, {}", token, req);
        return new AcpSchema.SetSessionModeResponse();
    }

    @Override
    public AcpSchema.SetSessionModelResponse handleSetSessionModel(Token token, AcpSchema.SetSessionModelRequest req) {
        log.debug("AcpSchema.SetSessionModelRequest: {}, {}", token, req);
        return new AcpSchema.SetSessionModelResponse();
    }
}
```

### 3. Create ACP-Enabled Controller

Use `@ApiMapping` with `acpAvailable=true` to enable ACP support for API endpoints. Access `AcpSessionContext` for
bidirectional communication:

```java

@ApiController(tags = {"acp"})
public class AcpController {

    @ApiMapping(path = "/authorization/create", authorization = false, acpAvailable = true,
            summary = "Retrieve JWT Token",
            description = "A utility tool that authenticates with the target service and returns a valid JWT token for subsequent API requests."
    )
    public CreateAuthorizationResponse createAuthorization(CreateAuthorizationRequest createAuthorizationRequest) {
        // Create tokens for ACP client
        Token refreshToken = Token.builder()
                .uid(createAuthorizationRequest.uid())
                .name(createAuthorizationRequest.name())
                .roles(List.of())
                .refresh(true)
                .build();
        String refreshTokenAuth = this.tokenProvider.createAuthorization(refreshToken);

        Token accessToken = refreshToken.createAccessToken();
        String accessTokenAuth = this.tokenProvider.createAuthorization(accessToken);

        return CreateAuthorizationResponse.builder()
                .accessToken(accessTokenAuth)
                .refreshToken(refreshTokenAuth)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @ApiMapping(path = "/toolCallProgress", acpAvailable = true)
    public TokenResponse toolCallProgress(Token token, AcpSessionContext acpSessionContext) {
        if (acpSessionContext != null) {
            log.info("toolCallProgress: {}", acpSessionContext.getSessionId());
            // Send progress notifications
            for (int i = 0; i < 3; i++) {
                JSONObject messageJson = new JSONObject();
                messageJson.put("data", i);
                AcpSchemaExt.ProgressNotification progressNotification = AcpSchemaExt.ProgressNotification.create(
                        i + 1, 3, XDataUtils.toJSONString(messageJson)
                );
                acpSessionContext.sendToolCallProgress(progressNotification);
            }
        }
        return TokenResponse.builder()
                .name("ok").build();
    }
}
```

## AcpSessionContext Capabilities

The `AcpSessionContext` provides rich capabilities for bidirectional communication with ACP clients:

### Interactive User Prompts

```java
// Ask for user permission
Boolean approved = acpSessionContext.askPermission(
                "Can you do this?", Duration.ofSeconds(30));

// Present choice options
List<String> colors = List.of("red", "green", "blue");
String result = acpSessionContext.askChoice(
        "What's your favorite color?", colors, Duration.ofSeconds(30));
```

### Command Execution

```java
// Execute system commands on client side
Command command = Command.of("uname", "-a");
CommandResult result = acpSessionContext.execute(command, Duration.ofSeconds(10));
```

### File Operations

```java
// Read file from client side
CommandResult readResult = acpSessionContext.readFile("/README.md", Duration.ofSeconds(10));

// Write file to client side
CommandResult writeResult = acpSessionContext.writeFile(
        "/README.md", "Hello, world!", Duration.ofSeconds(10));
```

## Configuration

### Default Configuration

The module provides auto-configuration with sensible defaults:

- WebSocket endpoint: `/acp`

## Best Practices

1. **Session State Management**: Store minimal session state on the server, leverage client-side capabilities
2. **Progress Updates**: Send frequent progress notifications for long-running operations
3. **Timeout Handling**: Always specify appropriate timeouts for interactive operations
4. **Error Propagation**: Use `ApiException` for consistent error handling across ACP and HTTP interfaces
5. **Security**: Validate all client inputs and enforce proper authorization checks

## Examples

See the test directory for comprehensive examples:

- `src/test/java/io/github/siyukio/application/acp/AcpSessionHandlerImpl.java` - Complete session handler implementation
- `src/test/java/io/github/siyukio/application/controller/AcpController.java` - ACP-enabled controller with all
  capabilities