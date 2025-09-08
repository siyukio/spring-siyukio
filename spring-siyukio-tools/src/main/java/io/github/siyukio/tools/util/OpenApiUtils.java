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

    public static JSONObject createArrayResponse(JSONObject arrayParamJson) {
        JSONObject requestParamJson = new JSONObject();
        //type
        requestParamJson.put("type", "array");
        //description
        String description = arrayParamJson.optString("description");
        requestParamJson.put("description", description);
        //items
        JSONObject itemsJson = new JSONObject();
        requestParamJson.put("items", itemsJson);

        JSONObject arrayItemJson = arrayParamJson.optJSONObject("items");
        String itemType = arrayItemJson.optString("type");
        if (itemType.equals("object")) {
            JSONArray itemsChildArray = arrayItemJson.optJSONArray("childArray");
            itemsJson = createObjectResponse(itemsChildArray);
        } else {
            itemsJson = new JSONObject();
        }
        requestParamJson.put("items", itemsJson);
        return requestParamJson;
    }

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
        JSONObject schemaJson = createObjectRequest(apiDefinition.requestParameters());
        applicationJson.put("schema", schemaJson);
        return requestBodyJson;
    }

    public static JSONObject createObjectRequest(JSONArray requestParameters) {
        JSONObject requestParamJson = new JSONObject();
        //type
        requestParamJson.put("type", "object");
        //required
        JSONArray requiredArray = new JSONArray();
        requestParamJson.put("required", requiredArray);
        //properties
        JSONObject propertiesJson = new JSONObject();
        requestParamJson.put("properties", propertiesJson);
        //
        JSONObject childParam;
        String type;
        boolean required;
        String name;
        String description;
        JSONObject propJson;
        JSONArray itemsChildArray;
        boolean pwd;
        for (int index = 0; index < requestParameters.length(); index++) {
            childParam = requestParameters.getJSONObject(index);
            type = childParam.optString("type");
            required = childParam.optBoolean("required");
            name = childParam.optString("name");
            description = childParam.optString("description");
            if (required) {
                requiredArray.put(name);
            }
            switch (type) {
                case "object":
                    itemsChildArray = childParam.optJSONArray("childArray");
                    propJson = createObjectRequest(itemsChildArray);
                    break;
                case "array":
                    propJson = createArrayRequest(childParam);
                    break;
                default:
                    propJson = new JSONObject();
                    if (type.equals("date")) {
                        propJson.put("type", "string");
                    } else {
                        propJson.put("type", type);
                    }
                    if (childParam.has("minimum")) {
                        propJson.put("minimum", childParam.opt("minimum"));
                    }
                    if (childParam.has("maximum")) {
                        propJson.put("maximum", childParam.opt("maximum"));
                    }
                    if (childParam.has("format")) {
                        propJson.put("format", childParam.opt("format"));
                    }
                    if (childParam.has("pattern")) {
                        propJson.put("pattern", childParam.opt("pattern"));
                    }
                    if (childParam.has("maxLength")) {
                        propJson.put("maxLength", childParam.opt("maxLength"));
                    }
                    if (childParam.has("minLength")) {
                        propJson.put("minLength", childParam.opt("minLength"));
                    }
                    if (childParam.has("example")) {
                        propJson.put("example", childParam.opt("example"));
                    }
                    if (childParam.has("examples")) {
                        propJson.put("examples", childParam.opt("examples"));
                    }
                    if (childParam.has("default")) {
                        propJson.put("default", childParam.opt("default"));
                    }

                    propJson.put("description", description);

                    break;
            }
            propertiesJson.put(name, propJson);
        }
        return requestParamJson;
    }

    public static JSONObject createArrayRequest(JSONObject arrayParamJson) {
        JSONObject requestParamJson = new JSONObject();
        //type
        requestParamJson.put("type", "array");
        //description
        String description = arrayParamJson.optString("description");
        requestParamJson.put("description", description);
        //items
        JSONObject itemsJson = new JSONObject();
        requestParamJson.put("items", itemsJson);

        JSONObject arrayItemJson = arrayParamJson.optJSONObject("items");
        String itemType = arrayItemJson.optString("type");
        if (itemType.equals("object")) {
            JSONArray itemsChildArray = arrayItemJson.optJSONArray("childArray");
            itemsJson = createObjectRequest(itemsChildArray);
        } else {
            itemsJson = new JSONObject();
            itemsJson.put("type", itemType);
        }
        requestParamJson.put("items", itemsJson);
        if (arrayParamJson.has("maxItems")) {
            requestParamJson.put("maxItems", arrayParamJson.opt("maxItems"));
        }
        if (arrayParamJson.has("minItems")) {
            requestParamJson.put("minItems", arrayParamJson.opt("minItems"));
        }
        return requestParamJson;
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
        JSONObject schemaJson = createObjectResponse(apiDefinition.responseParameters());
        applicationJson.put("schema", schemaJson);
        return responseJson;
    }

    public static JSONObject createObjectResponse(JSONArray responseParamArray) {
        JSONObject responseParamJson = new JSONObject();
        //type
        responseParamJson.put("type", "object");
        //required
        JSONArray requiredArray = new JSONArray();
        responseParamJson.put("required", requiredArray);
        //properties
        JSONObject propertiesJson = new JSONObject();
        responseParamJson.put("properties", propertiesJson);
        //
        JSONObject childParam;
        String type;
        String name;
        String description;
        JSONObject propJson;
        JSONArray itemsChildArray;

        for (int index = 0; index < responseParamArray.length(); index++) {
            childParam = responseParamArray.getJSONObject(index);
            type = childParam.optString("type");
            name = childParam.optString("name");
            description = childParam.optString("description");
            switch (type) {
                case "object":
                    itemsChildArray = childParam.optJSONArray("childArray");
                    propJson = createObjectResponse(itemsChildArray);
                    break;
                case "array":
                    propJson = createArrayResponse(childParam);
                    break;
                default:
                    propJson = new JSONObject();
                    if (type.equals("date")) {
                        propJson.put("type", "string");
                        propJson.put("format", "date");
                    } else {
                        propJson.put("type", type);
                    }
                    propJson.put("description", description);
                    break;
            }
            propertiesJson.put(name, propJson);
        }
        return responseParamJson;
    }

}
