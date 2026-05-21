package io.github.siyukio.tools.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Bugee
 */
public final class AcpSchemaExt {

    public static final String TRANSPORT_ID = "transport_id";

    public static final String METHOD_LIST_TOOLS = "list_tools";

    public static final String METHOD_CALL_TOOL = "call_tool";

    private AcpSchemaExt() {
    }

    public enum SessionConfigOptionCategory {

        @JsonProperty("mode")
        MODE,
        @JsonProperty("model")
        MODEL,
        @JsonProperty("thought_level")
        THOUGHT_LEVEL
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionInfoUpdate(
            @JsonProperty("sessionUpdate")
            String sessionUpdate,
            @JsonProperty("title")
            String title,
            @JsonProperty("updatedAt")
            String updatedAt,
            @JsonProperty("_meta")
            Map<String, Object> meta
    ) implements AcpSchema.SessionUpdate {
        public SessionInfoUpdate(String sessionUpdate, String title, String updatedAt) {
            this(sessionUpdate, title, updatedAt, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConfigOptionUpdate(
            @JsonProperty("sessionUpdate")
            String sessionUpdate,
            @JsonProperty("id")
            String id,
            @JsonProperty("name")
            String name,
            @JsonProperty("description")
            String description,
            @JsonProperty("category")
            SessionConfigOptionCategory category,
            @JsonProperty("_meta")
            Map<String, Object> meta
    ) implements AcpSchema.SessionUpdate {
        public ConfigOptionUpdate(String sessionUpdate, String id, String name) {
            this(sessionUpdate, id, name, null, null, null);
        }
    }

    public record TransportMessage(
            String transportId,
            AcpSchema.JSONRPCMessage jsonRpcMessage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProgressNotification(
            @JsonProperty("sessionUpdate")
            String sessionUpdate,
            @JsonProperty("progress")
            int progress,
            @JsonProperty("total")
            int total,
            @JsonProperty("message")
            String message,
            @JsonProperty("_meta")
            Map<String, Object> meta
    ) implements AcpSchema.SessionUpdate {

        public ProgressNotification(String sessionUpdate, int progress, int total, String message) {
            this(sessionUpdate, progress, total, message, null);
        }

        public ProgressNotification(String sessionUpdate, String message) {
            this(sessionUpdate, 1, 1, message, null);
        }
    }

    public record Tool(
            String name,
            String title,
            String description,
            JSONObject inputSchema,
            JSONObject outputSchema
    ) {
    }

    public record ListToolsResult(
            List<Tool> tools
    ) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ListToolsRequest(
            @JsonProperty("_meta")
            Map<String, Object> meta
    ) {
        public ListToolsRequest() {
            this(null);
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CallToolRequest(
            @JsonProperty("tool")
            String tool,
            @JsonProperty("toolCallId")
            String toolCallId,
            @JsonProperty("params")
            JSONObject params,
            @JsonProperty("_meta")
            Map<String, Object> meta
    ) {
        public CallToolRequest(String tool, String toolCallId, JSONObject params) {
            this(tool, toolCallId, params, null);
        }

    }
}
