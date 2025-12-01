package io.github.siyukio.postgresql.entity;

import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgIndex;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;
import lombok.Builder;
import lombok.With;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Bugee
 */
@PgEntity(schema = "test", comment = "record event", indexes = {
        @PgIndex(columns = {"type"}),
        @PgIndex(columns = {"error", "rating"}),
        @PgIndex(columns = {"teamId", "userId"}, unique = true)
})
@Builder
@With
public record RecordEvent(

        @PgKey
        String id,

        @PgColumn
        String type,

        @PgColumn
        String content,

        @PgColumn
        boolean error,

        @PgColumn
        double rating,

        @PgColumn
        int total,

        @PgColumn
        String teamId,

        @PgColumn
        String userId,

        @PgColumn(comment = "Elapsed time in milliseconds")
        long costMs,

        @PgColumn(comment = "Key attribute", defaultValue = """
                {"role":"user", "name":"hello"}
                """)
        JSONObject metadata,

        @PgColumn(comment = "Collection of messages")
        JSONArray messages,

        @PgColumn(comment = "item")
        Item item,

        @PgColumn(comment = "items")
        List<Item> items,

        @PgColumn
        LoginType loginType,

        @PgColumn
        LocalDateTime createdAt,

        @PgColumn
        long createdAtTs,

        @PgColumn
        LocalDateTime updatedAt,

        @PgColumn
        long updatedAtTs
) {

    public enum LoginType {
        USERNAME,
        PHONE,
        EMAIL,
        GOOGLE,
        APPLE
    }

    @Builder
    @With
    public record Item(
            String type,
            long costMs
    ) {
    }
}
