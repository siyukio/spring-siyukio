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

        String id,

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
        boolean refresh
) {

    public Token createAccessToken() {
        if (!this.refresh) {
            throw new IllegalStateException("Only refresh token can be converted to access token.");
        }
        return Token.builder()
                .uid(this.uid).name(this.name)
                .roles(this.roles).refresh(false)
                .build();
    }
}
