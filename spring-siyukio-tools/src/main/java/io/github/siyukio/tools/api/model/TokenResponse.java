package io.github.siyukio.tools.api.model;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Buddy
 */
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public class TokenResponse {

    @ApiParameter
    public String uid;

    @ApiParameter
    public String name;

    @ApiParameter
    public List<String> roles = new ArrayList<>();
}
