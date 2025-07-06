package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Bugee
 */
public class MyMcpSchema {

    public static final String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";

    private MyMcpSchema() {
    }


    // ---------------------------
    // Progress and Logging
    // ---------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProgressMessageNotification(// @formatter:off
                                       @JsonProperty("message") String message,
                                       @JsonProperty("progress") double progress,
                                       @JsonProperty("total") double total) {
    }// @formatter:on
}
