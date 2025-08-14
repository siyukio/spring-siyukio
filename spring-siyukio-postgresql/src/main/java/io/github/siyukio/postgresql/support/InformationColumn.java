package io.github.siyukio.postgresql.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Bugee
 */

public record InformationColumn(
        @JsonProperty("column_name")
        String columnName,

        @JsonProperty("data_type")
        String dataType,

        @JsonProperty("udt_name")
        String udtName,

        @JsonProperty("column_default")
        String columnDefault
) {
}
