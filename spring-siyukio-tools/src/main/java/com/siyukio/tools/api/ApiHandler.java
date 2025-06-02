package com.siyukio.tools.api;

import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.parameter.request.RequestValidator;
import com.siyukio.tools.api.parameter.response.ResponseFilter;
import lombok.ToString;

/**
 * @author Buddy
 */

@ToString
public class ApiHandler {

    public ApiDefinition apiDefinition;

    public RequestValidator requestValidator;

    public ResponseFilter responseFilter;

    public ApiInvoker apiInvoker;

}
