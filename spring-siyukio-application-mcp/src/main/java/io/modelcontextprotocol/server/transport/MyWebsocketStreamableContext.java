package io.modelcontextprotocol.server.transport;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Bugee
 */
@Getter
@Setter
public class MyWebsocketStreamableContext {

    private MyWebSocketStreamableServerTransportProvider myWebSocketStreamableServerTransportProvider;

    private MyWebSocketHandler myWebSocketHandler;
}
