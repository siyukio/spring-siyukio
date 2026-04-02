package io.github.siyukio.tools.acp;

import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import org.json.JSONObject;

import java.util.Map;


public record Invoke(
        String tool,
        String toolCallId,
        JSONObject params
) {

    public static Invoke create(String tool, JSONObject params) {
        return new Invoke(tool, IdUtils.getUniqueId(), params);
    }

    public static Invoke create(String tool, Map<String, Object> params) {
        return new Invoke(tool, IdUtils.getUniqueId(), new JSONObject(params));
    }

    public static Invoke create(String tool, Object params) {
        JSONObject paramsJson = XDataUtils.copy(params, JSONObject.class);
        return new Invoke(tool, IdUtils.getUniqueId(), paramsJson);
    }
}
