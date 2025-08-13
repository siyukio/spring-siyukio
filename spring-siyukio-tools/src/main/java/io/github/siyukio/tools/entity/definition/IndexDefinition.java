package io.github.siyukio.tools.entity.definition;

/**
 * @author Bugee
 */

public record IndexDefinition(
        String indexName,
        boolean unique,
        String[] columns
) {
}
