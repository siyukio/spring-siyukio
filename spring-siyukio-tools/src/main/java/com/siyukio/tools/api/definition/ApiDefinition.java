package com.siyukio.tools.api.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.json.JSONArray;

import java.util.List;

/**
 * @author Buddy
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDefinition {

    public List<String> paths;

    public String summary;

    public String description;

    public boolean deprecated;

    public List<String> tags;

    public List<String> roles;

    public boolean authorization;

    public boolean signature;

    public boolean mcpTool;

    public boolean sampling;

    public JSONArray requestParameters;

    public JSONArray responseParameters;

    public Class<?> returnType;

    public Class<?> realReturnType;

}
