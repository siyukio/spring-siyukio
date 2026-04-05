package io.github.siyukio.tools.acp;

import lombok.With;
import org.json.JSONObject;

import java.util.List;

/**
 *
 * @author Bugee
 */
public final class AcpSchemaExt {

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
