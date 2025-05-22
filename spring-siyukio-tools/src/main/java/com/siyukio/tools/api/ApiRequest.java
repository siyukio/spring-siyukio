package com.siyukio.tools.api;

import lombok.ToString;

import java.util.Map;

/**
 * Author: Buddy
 */

@ToString
public class ApiRequest {

    public final Map<String, String> parameterMap;

    public final String ip;

    public final String body;

    public final String userAgent;

    public ApiRequest(Map<String, String> parameterMap, String ip, String body, String userAgent) {
        this.parameterMap = parameterMap;
        this.ip = ip;
        this.body = body;
        this.userAgent = userAgent;
    }
}
