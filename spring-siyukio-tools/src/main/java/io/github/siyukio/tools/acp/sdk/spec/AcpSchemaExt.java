package io.github.siyukio.tools.acp.sdk.spec;

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

    public static final String AGENT_NAME = "agentName";

    public static final String TRANSPORT_ID = "transportId";

    public static final String METHOD_TOOL_LIST = "tool/list";

    public static final String METHOD_TOOL_CALL = "tool/call";

    private AcpSchemaExt() {
    }

    public static String apiToTool(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = path.replace("/", ".");
        return path;
    }

    /**
     * Get the meta from the given SessionUpdate by iterating through all known
     * SessionUpdate implementations in both AcpSchema and AcpSchemaExt.
     * @param sessionUpdate the SessionUpdate instance
     * @return the meta map, or null if not set or unknown type
     */
    public static Map<String, Object> getSessionUpdateMeta(AcpSchema.SessionUpdate sessionUpdate) {
        if (sessionUpdate instanceof SessionInfoUpdate info) {
            return info.meta();
        }
        else if (sessionUpdate instanceof ConfigOptionUpdate config) {
            return config.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.UserMessageChunk msg) {
            return msg.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.AgentMessageChunk msg) {
            return msg.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.AgentThoughtChunk thought) {
            return thought.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.ToolCall tool) {
            return tool.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.ToolCallUpdateNotification notification) {
            return notification.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.Plan plan) {
            return plan.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.AvailableCommandsUpdate update) {
            return update.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.CurrentModeUpdate update) {
            return update.meta();
        }
        else if (sessionUpdate instanceof AcpSchema.UsageUpdate update) {
            return update.meta();
        }
        return null;
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

            @JsonProperty("progress")
            int progress,
            @JsonProperty("total")
            int total,
            @JsonProperty("message")
            String message,
            @JsonProperty("_meta")
            Map<String, Object> meta
    ) {

        public ProgressNotification(int progress, int total, String message) {
            this(progress, total, message, null);
        }

        public ProgressNotification(String message) {
            this(100, 100, message, null);
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
