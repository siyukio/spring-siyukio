package io.github.siyukio.postgresql.entity;

import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgIndex;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

/**
 * @author Bugee
 */
@PgEntity(schema = "test", comment = "test event", indexes = {
        @PgIndex(columns = {"type"}),
        @PgIndex(columns = {"error", "rating"})
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TestEvent {

    @PgKey
    public String id;

    @PgColumn
    public String type;

    @PgColumn
    public String content;

    @PgColumn
    public boolean error;

    @PgColumn
    public double rating;

    @PgColumn
    public int total;

    @PgColumn(comment = "Elapsed time in milliseconds")
    public long times;

    @PgColumn(comment = "Key attribute", defaultValue = "{\"role\":\"user\"}")
    public JSONObject metadata;

    @PgColumn(comment = "Collection of messages")
    public JSONArray messages;

    @PgColumn
    public Date createAt;

    @PgColumn
    public long createTime;
}
