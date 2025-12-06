package io.github.siyukio.tools.util;

import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.definition.ApiDefinitionManager;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.SortedMap;

/**
 * @author Buddy
 */
@Slf4j
public abstract class OpenApiUtils {

    @Autowired
    private ApiDefinitionManager apiDefinitionManager;

    public static JSONObject createOpenApi(String host, SortedMap<String, ApiDefinition> sortedMap) {
        //OpenApi
        JSONObject openApiJson = new JSONObject();
        openApiJson.put("openapi", "3.0.0");

        //OpenApi.info
        JSONObject infoJson = new JSONObject();
        infoJson.put("title", ApiProfiles.IP4);
        infoJson.put("version", "last");
        openApiJson.put("info", infoJson);

        //OpenApi.server
        JSONObject serverJson = new JSONObject();
        String url;
        if (host.isEmpty()) {
            url = "http://" + ApiProfiles.IP4;
            if (ApiProfiles.PORT > -1 && ApiProfiles.PORT != 80 && ApiProfiles.PORT != 443) {
                url += ":" + ApiProfiles.PORT;
            }
        } else {
            url = "http://" + host;
        }
        url += ApiProfiles.CONTEXT_PATH;
        serverJson.put("url", url);
        serverJson.put("description", "");
        JSONArray serverArray = new JSONArray();
        serverArray.put(serverJson);
        openApiJson.put("servers", serverArray);
        //OpenApi.Paths
        JSONObject pathsJson = new JSONObject();
        JSONObject pathItemJson;
        for (Map.Entry<String, ApiDefinition> entry : sortedMap.entrySet()) {
            pathItemJson = createPathItem(entry.getValue());
            pathsJson.put(entry.getKey(), pathItemJson);
        }
        openApiJson.put("paths", pathsJson);
        return openApiJson;
    }

    public static JSONObject createPathItem(ApiDefinition apiDefinition) {
        //pathItem
        JSONObject pathItemJson = new JSONObject();

        //pathItem.post
        JSONObject postJson = new JSONObject();
        pathItemJson.put("post", postJson);

        //pathItem.post.parameters
        JSONArray parameters = createGlobalParameters(apiDefinition);
        postJson.put("parameters", parameters);

        //pathItem.post.requestBody
        JSONObject requestBodyJson = createRequestBody(apiDefinition);
        postJson.put("requestBody", requestBodyJson);

        //pathItem.post.responses
        JSONObject responsesJson = createResponses(apiDefinition);
        postJson.put("responses", responsesJson);

        //pathItem.post.deprecated
        postJson.put("deprecated", apiDefinition.deprecated());

        //pathItem.post.tags
        postJson.put("tags", apiDefinition.tags());
        //pathItem.post.summary
        postJson.put("summary", apiDefinition.summary());
        //pathItem.post.description
        postJson.put("description", apiDefinition.description());
        return pathItemJson;
    }

    public static JSONArray createGlobalParameters(ApiDefinition apiDefinition) {
        JSONArray parameters = new JSONArray();
        if (apiDefinition.authorization()) {
            JSONObject authHeader = new JSONObject();
            authHeader.put("name", "Authorization");
            authHeader.put("in", "header");
            authHeader.put("description", "jwt token");
            authHeader.put("required", true);
            authHeader.put("type", "string");
            parameters.put(authHeader);
        }
        return parameters;
    }

    public static JSONObject createRequestBody(ApiDefinition apiDefinition) {
        JSONObject requestBodyJson = new JSONObject();
        //content
        JSONObject requestBodyContentJson = new JSONObject();
        requestBodyJson.put("content", requestBodyContentJson);

        //content.application/json
        JSONObject applicationJson = new JSONObject();
        requestBodyContentJson.put("application/json", applicationJson);

        //content.application/json.schema
        applicationJson.put("schema", apiDefinition.requestBodyParameter().schema());
        return requestBodyJson;
    }

    public static JSONObject createResponses(ApiDefinition apiDefinition) {
        JSONObject responseJson = new JSONObject();
        //200
        JSONObject successJson = new JSONObject();
        responseJson.put("200", successJson);
        successJson.put("description", "ok");

        //content
        JSONObject requestBodyContentJson = new JSONObject();
        successJson.put("content", requestBodyContentJson);

        //content.application/json
        JSONObject applicationJson = new JSONObject();
        requestBodyContentJson.put("application/json", applicationJson);

        //content.application/json.schema
        applicationJson.put("schema", apiDefinition.responseBodyParameter().schema());
        return responseJson;
    }

}
