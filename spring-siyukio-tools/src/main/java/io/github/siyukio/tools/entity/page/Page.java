package io.github.siyukio.tools.entity.page;

import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * @author Bugee
 */
@Builder
@With
public record Page<T>(

        int total,

        List<T> items
) {
}
