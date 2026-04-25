---
name: siyukio-create-acp
description: "Generate ACP (Agent Client Protocol) Session Handler for Siyukio-based Spring Boot applications, enabling quick agent service implementation"
triggers:
  - "add acp handler"
  - "create acp handler"
  - "new acp handler"
  - "acp handler add"
  - "acp handler create"
  - "add agent"
  - "create agent"
  - "new agent"
  - "agent add"
  - "agent create"
---

<Purpose>
Generate ACP Session Handler for Siyukio Spring Boot applications. ACP (Agent Client Protocol) enables building AI agent services with bidirectional communication via WebSocket. The Session Handler manages agent session lifecycle, model/mode states, and tool execution.
</Purpose>

<Use_When>

- Creating a new ACP agent service
- Implementing session management for AI agents
- Setting up WebSocket-based ACP transport
- Handling tool calls from ACP clients

</Use_When>

<Prerequisites>

- Target module must exist: `{project-name}/{project-name}-domain-agent/`
- Module must have `spring-siyukio-application-acp` dependency in pom.xml
- Target file location: `src/main/java/{package-path}/agent/application/acp/`
- ACP server auto-configuration is enabled by default in Siyukio

</Prerequisites>

<Execution_Protocol>

## Step 1: Determine Agent Requirements

From the argument, extract:

- `{Agent}`: Agent name (PascalCase, e.g., `Assistant`, `CodingAgent`)
- `{agent}`: Agent variable name (camelCase, e.g., `assistant`)
- Tool names: which tools the agent will support
- Session features: model state, mode state, or both

## Step 2: Verify/Add Maven Dependency

Ensure `{project-name}-domain-agent/pom.xml` has:

```xml

<dependency>
    <groupId>io.github.siyukio</groupId>
    <artifactId>spring-siyukio-application-acp</artifactId>
</dependency>
```

## Step 3: Implement AcpSessionHandler

Location:
`{project-name}/{project-name}-domain-agent/src/main/java/{package-path}/agent/application/acp/AcpSessionHandlerImpl.java`

The ACP Session Handler manages session lifecycle and responds to client requests. It uses the provided `Token` for user
authentication.

### Template

```java
package

{package-name}.agent.application.acp;

import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.api.token.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ACP Session Handler for {Agent}.
 */
@Slf4j
@Service
public class AcpSessionHandlerImpl implements AcpSessionHandler {

    @Override
    public AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        log.debug("NewSessionRequest: {}, {}", token, req);
        // Initialize session state
        AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
        AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
        return new AcpSchema.NewSessionResponse(token.id(), sessionModeState, sessionModelState);
    }

    @Override
    public AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        log.debug("LoadSessionRequest: {}, {}", token, req);
        // Verify session belongs to user
        if (req.sessionId().equals(token.id())) {
            // Restore session state (from database or cache if needed)
            AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
            AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
            return new AcpSchema.LoadSessionResponse(sessionModeState, sessionModelState);
        }
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Session not found: " + req.sessionId());
    }

    @Override
    public void handleCancel(Token token, AcpSchema.CancelNotification req) {
        log.debug("CancelNotification: {}, {}", token, req);
        // Handle cancellation: stop ongoing operations, clean up resources
    }

    @Override
    public AcpSchema.SetSessionModeResponse handleSetSessionMode(Token token, AcpSchema.SetSessionModeRequest req) {
        log.debug("SetSessionModeRequest: {}, {}", token, req);
        return new AcpSchema.SetSessionModeResponse();
    }

    @Override
    public AcpSchema.SetSessionModelResponse handleSetSessionModel(Token token, AcpSchema.SetSessionModelRequest req) {
        log.debug("SetSessionModelRequest: {}, {}", token, req);
        return new AcpSchema.SetSessionModelResponse();
    }
}
```

</Execution_Protocol>

<Key_Conventions>

| Item          | Convention                                                  |
|---------------|-------------------------------------------------------------|
| Package       | `{package-name}.agent.application.acp`                      |
| Package Path  | `{package-path}/agent/application/acp/`                      |
| Handler Class | `AcpSessionHandlerImpl.java` implements `AcpSessionHandler` |
| Annotation    | `@Slf4j` + `@Service`                                       |
| Token         | First parameter for all handler methods                     |
| Session ID    | Use `token.id()` as session identifier                      |

</Key_Conventions>

<Handler_Methods>

### Session Lifecycle Methods

| Method              | Purpose                                    |
|---------------------|--------------------------------------------|
| `handleNewSession`  | Create new session, return initial state   |
| `handleLoadSession` | Load existing session by ID, restore state |
| `handleCancel`      | Handle client cancellation request         |

### State Management Methods

| Method                  | Purpose                                           |
|-------------------------|---------------------------------------------------|
| `handleSetSessionMode`  | Update session mode (e.g., "creative", "precise") |
| `handleSetSessionModel` | Update session model (e.g., "gpt-4", "claude-3")  |

</Handler_Methods>

<AcpSchema_Types>

### SessionModelState

```java
new AcpSchema.SessionModelState(String model, List<String> capabilities)
```

| Field          | Description                                |
|----------------|--------------------------------------------|
| `model`        | Model identifier (e.g., "gpt-4", "claude") |
| `capabilities` | Supported capabilities list                |

### SessionModeState

```java
new AcpSchema.SessionModeState(String mode, List<String> options)
```

| Field     | Description                               |
|-----------|-------------------------------------------|
| `mode`    | Mode identifier (e.g., "default", "code") |
| `options` | Available mode options                    |

### Response Types

| Type                      | Return Type               | Purpose             |
|---------------------------|---------------------------|---------------------|
| `NewSessionResponse`      | `NewSessionResponse`      | Session ID + states |
| `LoadSessionResponse`     | `LoadSessionResponse`     | Restored states     |
| `SetSessionModeResponse`  | `SetSessionModeResponse`  | Empty success       |
| `SetSessionModelResponse` | `SetSessionModelResponse` | Empty success       |

</AcpSchema_Types>

<Common_Patterns>

## Session State Persistence

To persist session state across server restarts:

```java

@Override
public AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
    // Query session from database/cache
    SessionData session = sessionService.findById(req.sessionId());
    if (session == null || !session.getUserId().equals(token.id())) {
        throw new AcpProtocolException(HttpStatus.NOT_FOUND.value(), "Session not found");
    }
    return new AcpSchema.LoadSessionResponse(
            session.getModeState(),
            session.getModelState()
    );
}
```

## Tool Execution Handling

Handle tool calls via `AcpSessionContext`:

```java
// In controller or tool handler
@Autowired
private AcpSessionContext sessionContext;

// Send progress to client
sessionContext.

sendProgress(sessionId, "Processing...",50);

// Send final result
sessionContext.

sendResult(sessionId, resultObject);
```

## Session Cleanup on Cancel

```java

@Override
public void handleCancel(Token token, AcpSchema.CancelNotification req) {
    log.info("Session cancelled: {}", req.sessionId());
    // Stop pending operations
    operationService.cancelBySession(req.sessionId());
    // Clean up temporary resources
    tempResourceService.cleanup(req.sessionId());
}
```

</Common_Patterns>

<Verification>
After implementation:
1. Run `./mvnw compile` to verify code compiles
2. Implement `AcpSessionHandler` interface methods
3. Test session persistence if implemented
</Verification>
