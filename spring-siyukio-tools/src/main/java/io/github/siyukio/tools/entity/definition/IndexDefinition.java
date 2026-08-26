package io.github.siyukio.tools.entity.definition;

/**
 * @author Bugee
 */

public record IndexDefinition(
        String indexName,
        String[] columns,
        boolean unique,
        boolean gin
) {

    public IndexDefinition {
        if (unique && gin) {
            throw new IllegalArgumentException("Index cannot be both unique and gin.");
        }
    }
}
