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

package io.github.siyukio.client.boot.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * @author Bugee
 */
@ConfigurationProperties(SiyukioMcpClientCommonProperties.CONFIG_PREFIX)
public class SiyukioMcpClientCommonProperties {

    public static final String CONFIG_PREFIX = "spring.ai.mcp.client";

    /**
     * The name of the MCP client instance.
     * <p>
     * This name is reported to clients and used for compatibility checks.
     */
    private String name = "spring-ai-mcp-client";

    /**
     * The version of the MCP client instance.
     * <p>
     * This version is reported to clients and used for compatibility checks.
     */
    private String version = "0.12.1";

    /**
     * The timeout duration for MCP client requests.
     * <p>
     * Defaults to 20 seconds.
     */
    private Duration requestTimeout = Duration.ofSeconds(20);

    private String baseUrl = "";

    private String mcpEndpoint = "/mcp";

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        Assert.notNull(baseUrl, "Base URL must not be null");
        this.baseUrl = baseUrl;
    }

    public String getMcpEndpoint() {
        return this.mcpEndpoint;
    }

    public void setMcpEndpoint(String sseEndpoint) {
        Assert.hasText(mcpEndpoint, "mcp endpoint must not be empty");
        this.mcpEndpoint = mcpEndpoint;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Duration getRequestTimeout() {
        return this.requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

}
