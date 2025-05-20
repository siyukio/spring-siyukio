package com.siyukio.tools.api.token;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Jwt token.
 *
 * @author Budyy
 */
@Data
@Builder
public final class Token {

    /**
     * User id
     */
    private String uid = "";

    /**
     * User name
     */
    private String name = "";

    /**
     * Authorization roles
     */
    private List<String> roles = new ArrayList<>();

    /**
     * Whether the token is used for refreshing the JWT token.
     */
    private boolean refreshing = false;

    /**
     * Whether the JWT token is expired.
     */
    private boolean expired = true;

}
