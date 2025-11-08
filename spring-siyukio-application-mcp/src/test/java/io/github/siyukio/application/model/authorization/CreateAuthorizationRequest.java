package io.github.siyukio.application.model.authorization;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author Buddy
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateAuthorizationRequest {

    @ApiParameter
    public String uid;

    @ApiParameter(required = false)
    public String name;

    @ApiParameter(required = false)
    public List<String> roles;
}
