package io.github.siyukio.application.controller;

import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.definition.ApiDefinitionManager;
import io.github.siyukio.tools.util.OpenApiUtils;
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
        if (ApiProfiles.DOCS) {
            SortedMap<String, ApiDefinition> sortedMap = new TreeMap<>(this.apiDefinitionManager.getApiDefinitionMap());
            return OpenApiUtils.createOpenApi(host, sortedMap);
        }

        return new JSONObject();
    }

}
