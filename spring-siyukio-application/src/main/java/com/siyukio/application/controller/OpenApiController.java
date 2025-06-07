package com.siyukio.application.controller;

import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.definition.ApiDefinitionManager;
import com.siyukio.tools.util.OpenApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.*;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Buddy
 */
@Slf4j
@Order
@RestController
public class OpenApiController {

    @Autowired
    private ApiDefinitionManager apiDefinitionManager;

    @CrossOrigin
    @RequestMapping(path = "/api-docs", method = {RequestMethod.GET})
    public JSONObject apiDocs(@RequestHeader(name = "HOST", required = false) String host) {

        SortedMap<String, ApiDefinition> sortedMap = new TreeMap<>(this.apiDefinitionManager.getApiDefinitionMap());
        return OpenApiUtils.createOpenApi(host, sortedMap);
    }

}
