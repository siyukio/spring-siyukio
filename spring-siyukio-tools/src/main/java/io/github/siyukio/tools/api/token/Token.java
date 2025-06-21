package io.github.siyukio.tools.api.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Jwt token.
 *
 * @author Budyy
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class Token {

    /**
     * session id
     */
    public String sid = "";

    /**
     * User id
     */
    public String uid = "";

    /**
     * User name
     */
    public String name = "";

    /**
     * Authorization roles
     */
    public List<String> roles = new ArrayList<>();

    /**
     * Whether the token is used for refreshing the JWT token.
     */
    public boolean refresh = false;

    /**
     * Whether the JWT token is expired.
     */
    public boolean expired = true;

}
