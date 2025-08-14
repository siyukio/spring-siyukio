package io.github.siyukio.postgresql.entity;

import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgIndex;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;
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
public class TestEvent {

    @PgKey(comment = "id")
    public String id;

    @PgColumn(comment = "type")
    public String type;

    @PgColumn(comment = "content")
    public String content;

    @PgColumn(comment = "is error")
    public boolean error;

    @PgColumn(comment = "Rating")
    public double rating;

    @PgColumn(comment = "Total count")
    public int total;

    @PgColumn(comment = "Elapsed time in milliseconds")
    public long times;

    @PgColumn(comment = "Key attribute", defaultValue = "{\"role\":\"user\"}")
    public JSONObject metadata;

    @PgColumn(comment = "Collection of messages")
    public JSONArray messages;

    @PgColumn(comment = "Creation time")
    public Date createAt;
}
