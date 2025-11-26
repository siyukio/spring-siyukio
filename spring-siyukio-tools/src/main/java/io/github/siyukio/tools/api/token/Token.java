package io.github.siyukio.tools.api.token;

import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Jwt token.
 *
 * @author Budyy
 */

@Builder
@With
public record Token(

        /*
          User id
         */
        String uid,

        /*
          User name
         */
        String name,

        /*
         * Authorization roles
         */
        List<String> roles,

        /*
         * Whether the token is used for refreshing the JWT token.
         */
        boolean refresh,

        /*
         * Whether the JWT token is expired.
         */
        boolean expired
) {
}
