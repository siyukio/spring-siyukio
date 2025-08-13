package io.github.siyukio.postgresql.entity;

import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;

/**
 * @author Bugee
 */
@PgEntity(schema = "test", comment = "test event")
public class TestEvent {

    @PgKey(comment = "id")
    public String id;

    @PgColumn(comment = "type")
    public String type;

    @PgColumn(comment = "content")
    public String content;
}
