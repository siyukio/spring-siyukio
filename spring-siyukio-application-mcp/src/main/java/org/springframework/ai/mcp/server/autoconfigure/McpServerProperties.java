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

package org.springframework.ai.mcp.server.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the Model Context Protocol (MCP) server.
 * <p>
 * These properties control the behavior and configuration of the MCP server, including:
 * <ul>
 * <li>Server identification (name and version)</li>
 * <li>Change notification settings for tools, resources, and prompts</li>
 * <li>Web transport endpoint configuration</li>
 * </ul>
 * <p>
 * All properties are prefixed with {@code spring.ai.mcp.server}.
 *
 * @author Christian Tzolov
 * @see McpServerAutoConfiguration
 * @since 1.0.0
 */
@ConfigurationProperties(McpServerProperties.CONFIG_PREFIX)
public class McpServerProperties {

    public static final String CONFIG_PREFIX = "spring.ai.mcp.server";

    /**
     * Enable/disable the MCP server.
     * <p>
     * When set to false, the MCP server and all its components will not be initialized.
     */
    private boolean enabled = true;

    /**
     * Enable/disable the standard input/output (stdio) transport.
     * <p>
     * When enabled, the server will listen for incoming messages on the standard input
     * and write responses to the standard output.
     */
    private boolean stdio = false;

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
    private String version = "0.10.0";

    /**
     * The instructions of the MCP server instance.
     * <p>
     * These instructions are used to provide guidance to the client on how to interact
     * with this server.
     */
    private String instructions = null;

    /**
     * Enable/disable notifications for resource changes. Only relevant for MCP servers
     * with resource capabilities.
     * <p>
     * When enabled, the server will notify clients when resources are added, updated, or
     * removed.
     */
    private boolean resourceChangeNotification = true;

    /**
     * Enable/disable notifications for tool changes. Only relevant for MCP servers with
     * tool capabilities.
     * <p>
     * When enabled, the server will notify clients when tools are registered or
     * unregistered.
     */
    private boolean toolChangeNotification = true;

    /**
     * Enable/disable notifications for prompt changes. Only relevant for MCP servers with
     * prompt capabilities.
     * <p>
     * When enabled, the server will notify clients when prompt templates are modified.
     */
    private boolean promptChangeNotification = true;

    /**
     *
     */
    private String baseUrl = "";

    /**
     *
     */
    private String sseEndpoint = "/sse";

    /**
     * The endpoint path for Server-Sent Events (SSE) when using web transports.
     * <p>
     * This property is only used when transport is set to WEBMVC or WEBFLUX.
     */
    private String sseMessageEndpoint = "/mcp/message";

    /**
     * The type of server to use for MCP server communication.
     * <p>
     * Supported types are:
     * <ul>
     * <li>SYNC - Standard synchronous server (default)</li>
     * <li>ASYNC - Asynchronous server</li>
     * </ul>
     */
    private ServerType type = ServerType.SYNC;

    private Capabilities capabilities = new Capabilities();

    /**
     * Sets the duration to wait for server responses before timing out requests. This
     * timeout applies to all requests made through the client, including tool calls,
     * resource access, and prompt operations.
     */
    private Duration requestTimeout = Duration.ofSeconds(20);
    /**
     * (Optional) response MIME type per tool name.
     */
    private Map<String, String> toolResponseMimeType = new HashMap<>();

    public Duration getRequestTimeout() {
        return this.requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        Assert.notNull(requestTimeout, "Request timeout must not be null");
        this.requestTimeout = requestTimeout;
    }

    public Capabilities getCapabilities() {
        return this.capabilities;
    }

    public boolean isStdio() {
        return this.stdio;
    }

    public void setStdio(boolean stdio) {
        this.stdio = stdio;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getInstructions() {
        return this.instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public boolean isResourceChangeNotification() {
        return this.resourceChangeNotification;
    }

    public void setResourceChangeNotification(boolean resourceChangeNotification) {
        this.resourceChangeNotification = resourceChangeNotification;
    }

    public boolean isToolChangeNotification() {
        return this.toolChangeNotification;
    }

    public void setToolChangeNotification(boolean toolChangeNotification) {
        this.toolChangeNotification = toolChangeNotification;
    }

    public boolean isPromptChangeNotification() {
        return this.promptChangeNotification;
    }

    public void setPromptChangeNotification(boolean promptChangeNotification) {
        this.promptChangeNotification = promptChangeNotification;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        Assert.notNull(baseUrl, "Base URL must not be null");
        this.baseUrl = baseUrl;
    }

    public String getSseEndpoint() {
        return this.sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        Assert.hasText(sseEndpoint, "SSE endpoint must not be empty");
        this.sseEndpoint = sseEndpoint;
    }

    public String getSseMessageEndpoint() {
        return this.sseMessageEndpoint;
    }

    public void setSseMessageEndpoint(String sseMessageEndpoint) {
        Assert.hasText(sseMessageEndpoint, "SSE message endpoint must not be empty");
        this.sseMessageEndpoint = sseMessageEndpoint;
    }

    public ServerType getType() {
        return this.type;
    }

    public void setType(ServerType serverType) {
        Assert.notNull(serverType, "Server type must not be null");
        this.type = serverType;
    }

    public Map<String, String> getToolResponseMimeType() {
        return this.toolResponseMimeType;
    }

    /**
     * Server types supported by the MCP server.
     */
    public enum ServerType {

        /**
         * Synchronous (McpSyncServer) server
         */
        SYNC,

        /**
         * Asynchronous (McpAsyncServer) server
         */
        ASYNC

    }

    public static class Capabilities {

        private boolean resource = true;

        private boolean tool = true;

        private boolean prompt = true;

        private boolean completion = true;

        public boolean isResource() {
            return this.resource;
        }

        public void setResource(boolean resource) {
            this.resource = resource;
        }

        public boolean isTool() {
            return this.tool;
        }

        public void setTool(boolean tool) {
            this.tool = tool;
        }

        public boolean isPrompt() {
            return this.prompt;
        }

        public void setPrompt(boolean prompt) {
            this.prompt = prompt;
        }

        public boolean isCompletion() {
            return this.completion;
        }

        public void setCompletion(boolean completion) {
            this.completion = completion;
        }

    }

}
