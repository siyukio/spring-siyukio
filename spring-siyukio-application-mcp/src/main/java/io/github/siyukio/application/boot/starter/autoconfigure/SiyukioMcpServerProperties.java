/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.siyukio.application.boot.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * @author Bugee
 */
@ConfigurationProperties(SiyukioMcpServerProperties.CONFIG_PREFIX)
public class SiyukioMcpServerProperties {

    public static final String CONFIG_PREFIX = "spring.ai.mcp.server";

    /**
     * The name of the MCP server instance.
     * <p>
     * This name is used to identify the server in logs and monitoring.
     */
    private String name = "mcp-server";

    /**
     * The version of the MCP server instance.
     * <p>
     * This version is reported to clients and used for compatibility checks.
     */
    private String version = "0.12.1";

    private String mcpEndpoint = "/mcp";

    /**
     * Sets the duration to wait for server responses before timing out requests. This
     * timeout applies to all requests made through the client, including tool calls,
     * resource access, and prompt operations.
     */
    private Duration requestTimeout = Duration.ofSeconds(20);

    public Duration getRequestTimeout() {
        return this.requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        Assert.notNull(requestTimeout, "Request timeout must not be null");
        this.requestTimeout = requestTimeout;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        Assert.hasText(version, "Version must not be empty");
        this.version = version;
    }


    public String getMcpEndpoint() {
        return this.mcpEndpoint;
    }

    public void setMcpEndpoint(String sseEndpoint) {
        Assert.hasText(mcpEndpoint, "mcp endpoint must not be empty");
        this.mcpEndpoint = mcpEndpoint;
    }

}
