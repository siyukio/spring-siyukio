package io.github.siyukio.tools.acp;

/**
 *
 * @author Bugee
 */
public final class AcpSchemaExt {

    public record ProgressNotification(
            int progress,

            int total,

            String message
    ) {
    }
}
