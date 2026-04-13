package io.github.siyukio.tools.acp;

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

    public static final String WS_SESSION_ID = "wsSessionId";

    public static final String LIST_TOOLS = "listTools";

    public static final String TOOL_CALL_UPDATE = "tool_call_update";

    public static final String AGENT_MESSAGE_CHUNK = "agent_message_chunk";

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

    /**
     * Cancel notification - cancels ongoing operations
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CancelNotification(
            @JsonProperty("sessionId")
            String sessionId,

            @JsonProperty("_meta")
            Map<String, Object> meta
    ) {
    }

    /**
     * Set session mode request
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SetSessionModeRequest(
            @JsonProperty("sessionId")
            String sessionId,

            @JsonProperty("modeId")
            String modeId,

            @JsonProperty("_meta")
            Map<String, Object> meta
    ) {
    }

    /**
     * Set session model request (UNSTABLE)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SetSessionModelRequest(
            @JsonProperty("sessionId")
            String sessionId,
            
            @JsonProperty("modelId")
            String modelId,

            @JsonProperty("_meta")
            Map<String, Object> meta
    ) {
    }
}
