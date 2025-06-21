package io.github.siyukio.application.model.signature;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Buddy
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignatureRequest {

    @ApiParameter
    public int num;

    @ApiParameter
    public String text;

    @ApiParameter
    public boolean bool;
}
