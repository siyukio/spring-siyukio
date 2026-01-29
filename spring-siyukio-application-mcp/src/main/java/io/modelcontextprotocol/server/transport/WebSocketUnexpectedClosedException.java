package io.modelcontextprotocol.server.transport;

import lombok.Getter;

public class WebSocketUnexpectedClosedException extends RuntimeException {

    @Getter
    private final String sessionId;

    public WebSocketUnexpectedClosedException(String sessionId) {
        super("WebSocket session unexpectedly closed: " + sessionId);
        this.sessionId = sessionId;
    }

    public WebSocketUnexpectedClosedException(String sessionId, Throwable cause) {
        super("WebSocket session unexpectedly closed: " + sessionId, cause);
        this.sessionId = sessionId;
    }

}
