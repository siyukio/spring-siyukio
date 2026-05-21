package com.agentclientprotocol.sdk.client;

import com.agentclientprotocol.sdk.json.TypeRef;
import com.agentclientprotocol.sdk.spec.AcpClientTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.agentclientprotocol.sdk.spec.AcpSession;
import com.agentclientprotocol.sdk.util.Assert;
import io.github.siyukio.tools.acp.AcpSchemaExt;
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

    private final AcpSession session;

    AcpAsyncClientExt(AcpSession session, AcpClientTransport transport, AcpSchema.ClientCapabilities clientCapabilities) {
        super(session, transport, clientCapabilities);
        this.session = session;
    }

    public Mono<AcpSchemaExt.ListToolsResult> listTools(AcpSchemaExt.ListToolsRequest listToolsRequest) {
        Assert.notNull(listToolsRequest, "ListTools request must not be null");
        return session.sendRequest(AcpSchemaExt.METHOD_LIST_TOOLS, listToolsRequest, LIST_TOOLS_RESPONSE_TYPE_REF);
    }

    public Mono<JSONObject> callTool(AcpSchemaExt.CallToolRequest callToolRequest) {
        Assert.notNull(callToolRequest, "CallTool request must not be null");
        return session.sendRequest(AcpSchemaExt.METHOD_CALL_TOOL, callToolRequest, CALL_TOOL_RESPONSE_TYPE_REF);
    }
}
