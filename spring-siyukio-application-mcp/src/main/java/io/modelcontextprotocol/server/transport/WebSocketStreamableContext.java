package io.modelcontextprotocol.server.transport;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Bugee
 */
@Getter
@Setter
public class WebSocketStreamableContext {

    private WebSocketStreamableServerTransportProvider webSocketStreamableServerTransportProvider;

    private WebSocketHandler webSocketHandler;
}
