package io.github.siyukio.postgresql.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Bugee
 */

public record InformationIndex(
        @JsonProperty("indexname")
        String indexName,

        @JsonProperty("indexdef")
        String indexDef
) {
}
