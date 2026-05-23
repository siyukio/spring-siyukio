package com.agentclientprotocol.sdk.client;

import com.agentclientprotocol.sdk.json.TypeRef;
import com.agentclientprotocol.sdk.spec.AcpClientSession;
import com.agentclientprotocol.sdk.spec.AcpClientTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.agentclientprotocol.sdk.spec.AcpSessionExt;
import com.agentclientprotocol.sdk.util.Assert;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import reactor.core.publisher.Mono;

/**
 *
 * @author Bugee
 */
@Slf4j
public class AcpAsyncClientExt extends AcpAsyncClient {

    private static final TypeRef<AcpSchemaExt.ListToolsResult> LIST_TOOLS_RESPONSE_TYPE_REF = new TypeRef<>() {
    };

    private static final TypeRef<JSONObject> CALL_TOOL_RESPONSE_TYPE_REF = new TypeRef<>() {
    };

    private final AcpSessionExt session;

    AcpAsyncClientExt(AcpSessionExt session, AcpClientTransport transport, AcpSchema.ClientCapabilities clientCapabilities) {
        super(session, transport, clientCapabilities);
        this.session = session;
    }

    public Mono<AcpSchemaExt.ListToolsResult> listTools(AcpSchemaExt.ListToolsRequest listToolsRequest) {
        Assert.notNull(listToolsRequest, "ListTools request must not be null");
        return session.sendRequest(AcpSchemaExt.METHOD_TOOL_LIST, listToolsRequest, LIST_TOOLS_RESPONSE_TYPE_REF);
    }

    public Mono<JSONObject> callTool(AcpSchemaExt.CallToolRequest callToolRequest, AcpClientSession.NotificationHandler notificationHandler) {
        Assert.notNull(callToolRequest, "CallTool request must not be null");
        if (notificationHandler == null) {
            return session.sendRequest(AcpSchemaExt.METHOD_TOOL_CALL, callToolRequest, CALL_TOOL_RESPONSE_TYPE_REF);
        }

        this.session.putNotificationHandler(callToolRequest.toolCallId(), notificationHandler);
        return session.sendRequest(AcpSchemaExt.METHOD_TOOL_CALL, callToolRequest, CALL_TOOL_RESPONSE_TYPE_REF)
                .doFinally(signalType -> {
                    this.session.removeNotification(callToolRequest.toolCallId());
                });
    }
}
