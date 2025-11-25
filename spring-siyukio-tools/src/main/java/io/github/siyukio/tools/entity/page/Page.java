package io.github.siyukio.tools.entity.page;

import java.util.List;

/**
 * @author Bugee
 */
public record Page<T>(

        int total,
        
        List<T> items
) {
}
