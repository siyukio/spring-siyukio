package io.github.siyukio.client.boot.starter.autoconfigure;


import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Bugee
 */

@EnableConfigurationProperties(McpClientCommonProperties.class)
public class MyMcpClientConfiguration {
}
