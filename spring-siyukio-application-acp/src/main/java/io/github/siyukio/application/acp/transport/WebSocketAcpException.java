package io.github.siyukio.application.acp.transport;

import lombok.Getter;

public class WebSocketAcpException extends RuntimeException {

    @Getter
    private final String sessionId;

    public WebSocketAcpException(String sessionId, Throwable cause) {
        super("WebSocket Acp session unexpectedly closed: " + sessionId, cause);
        this.sessionId = sessionId;
    }

}
