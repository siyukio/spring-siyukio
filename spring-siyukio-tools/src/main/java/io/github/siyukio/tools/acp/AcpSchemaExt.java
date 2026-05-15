package io.github.siyukio.tools.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Bugee
 */
public final class AcpSchemaExt {

    public static final String DEFAULT_AUTH_METHOD_NAME = "jwt_id";

    public static final String TRANSPORT_ID = "transport_id";

    public static final String LIST_TOOLS = "list_tools";

    public static final String TOOL_CALL_UPDATE = "tool_call_update";

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

    @With
    public record ProgressNotification(
            String toolCallId,

            int progress,

            int total,

            String message
    ) {

        public static ProgressNotification create(int progress, int total, String message) {
            return new ProgressNotification(null, progress, total, message);
        }

        public static ProgressNotification create(String message) {
            return new ProgressNotification(null, 1, 1, message);
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
}
