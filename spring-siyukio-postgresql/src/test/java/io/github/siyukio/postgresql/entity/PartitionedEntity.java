package io.github.siyukio.postgresql.entity;

import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.postgresql.annotation.PgColumn;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.entity.postgresql.annotation.PgIndex;
import io.github.siyukio.tools.entity.postgresql.annotation.PgKey;
import lombok.Builder;
import lombok.With;

import java.time.LocalDateTime;

/**
 * @author Bugee
 */
@PgEntity(schema = "test", comment = "partitioned entity",
        partition = EntityDefinition.Partition.HOUR,
        indexes = {
                @PgIndex(columns = {"message", "createdAtTs"})
        })
@Builder
@With
public record PartitionedEntity(

        @PgKey
        String id,

        @PgColumn
        String type,

        @PgColumn
        String message,

        @PgColumn
        String salt,

        @PgColumn
        LocalDateTime createdAt,

        @PgColumn
        long createdAtTs,

        @PgColumn
        LocalDateTime updatedAt,

        @PgColumn
        long updatedAtTs
) {
}
