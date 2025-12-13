package io.github.siyukio.tools.api;

import lombok.Builder;
import lombok.With;

import java.util.Map;

/**
 * Author: Buddy
 */

@Builder
@With
public record ApiRequest(

        Map<String, String> parameters,

        Map<String, String> headers,

        String ip,

        String body,

        String userAgent
) {
}
