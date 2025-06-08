package com.siyukio.application.model.authorization;

import com.siyukio.tools.api.annotation.ApiParameter;
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

    @ApiParameter
    public String name;

    @ApiParameter
    public List<String> roles;
}
