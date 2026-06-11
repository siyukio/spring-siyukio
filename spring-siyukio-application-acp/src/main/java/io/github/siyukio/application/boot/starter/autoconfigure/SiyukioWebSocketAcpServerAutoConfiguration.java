package io.github.siyukio.application.boot.starter.autoconfigure;


import com.agentclientprotocol.sdk.agent.AcpAsyncAgent;
import com.agentclientprotocol.sdk.agent.SimpleAcpAgent;
import com.agentclientprotocol.sdk.agent.transport.SpringWebSocketAcpTransport;
import io.github.siyukio.application.acp.AcpSessionHandler;
import io.github.siyukio.tools.acp.annotation.Agent;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.token.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bugee
 */
@Import(DelegatingWebSocketConfiguration.class)
@AutoConfigureAfter({SiyukioApplicationAutoConfiguration.class})
@Slf4j
public class SiyukioWebSocketAcpServerAutoConfiguration implements WebSocketConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * Resolves the agent name from @Agent annotation attributes.
     * Checks both 'value' and 'name' aliases.
     */
    private String resolveAgentName(Agent annotation) {
        // Both value() and name() are aliases for each other,
        // so we can use either one
        String name = annotation.value();
        return name != null ? name.trim() : "";
    }

    @Bean
    public SpringWebSocketAcpTransport webSocketAcpTransport(ObjectProvider<AcpSessionHandler> acpSessionHandlerProvider) {
        Map<String, AcpSessionHandler> handlerMap = new HashMap<>();
        acpSessionHandlerProvider.stream().forEach(handler -> {
            Agent annotation = handler.getClass().getAnnotation(Agent.class);

            if (annotation != null) {
                // Use the value/name attribute as the agent identifier
                String agentName = resolveAgentName(annotation);

                if (!agentName.isEmpty()) {
                    if (handlerMap.containsKey(agentName)) {
                        log.warn("Duplicate ACP agent name '{}' detected: {} and {}",
                                agentName,
                                handlerMap.get(agentName).getClass().getName(),
                                handler.getClass().getName());
                    }
                    handlerMap.put(agentName, handler);
                } else {
                    // Default handler (no name specified)
                    handlerMap.put("", handler);
                }

                log.info("Registered ACP agent: '{}' -> {}",
                        agentName.isEmpty() ? "(default)" : agentName,
                        handler.getClass().getName());
            } else {
                // Fallback: handlers without @Agent annotation become default
                log.warn("AcpSessionHandler '{}' without @Agent annotation will be used as default",
                        handler.getClass().getName());
                handlerMap.putIfAbsent("", handler);
            }
        });

        if (handlerMap.isEmpty()) {
            log.warn("""
                    
                    No AcpSessionHandler bean found.
                    It is recommended to register at least one @Agent annotated bean.
                    Example:
                    
                        @Slf4j
                        @Agent
                        public class MyAgentAcpSessionHandler implements AcpSessionHandler { ... }
                    """);
            handlerMap.put("", new AcpSessionHandler() {
            });
        }
        if (!handlerMap.containsKey("")) {
            handlerMap.put("", new AcpSessionHandler() {
            });
        }

        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        return new SpringWebSocketAcpTransport(tokenProvider, handlerMap);
    }

    @Bean
    public AcpAsyncAgent acpAsyncAgent() {
        SpringWebSocketAcpTransport springWebSocketAcpTransport = this.applicationContext.getBean(SpringWebSocketAcpTransport.class);
        AipHandlerManager aipHandlerManager = this.applicationContext.getBean(AipHandlerManager.class);
        SpringWebSocketAcpTransport.AcpToolHandler acpToolHandler = springWebSocketAcpTransport.new AcpToolHandler(aipHandlerManager.getApiHandlerMap());

        SimpleAcpAgent.AsyncAgentBuilder builder = SimpleAcpAgent.async(springWebSocketAcpTransport)
                .initializeHandler(springWebSocketAcpTransport.new AcpInitializeHandler())
                .newSessionHandler(springWebSocketAcpTransport.new AcpNewSessionHandler())
                .loadSessionHandler(springWebSocketAcpTransport.new AcpLoadSessionHandler())
                .cancelHandler(springWebSocketAcpTransport.new AcpCancelHandler())
                .setSessionModeHandler(springWebSocketAcpTransport.new AcpSetSessionModeHandler())
                .setSessionModelHandler(springWebSocketAcpTransport.new AcpSetSessionModelHandler())
                .promptHandler(springWebSocketAcpTransport.new AcpPromptHandler())
                .listToolsHandler(acpToolHandler)
                .callToolHandler(acpToolHandler);
        AcpAsyncAgent acpAsyncAgent = builder.build();
        acpAsyncAgent.start().subscribe();
        log.info("Start WebSocket Acp server {}", acpAsyncAgent.getClientCapabilities());
        return acpAsyncAgent;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        SpringWebSocketAcpTransport springWebSocketAcpTransport = this.applicationContext.getBean(SpringWebSocketAcpTransport.class);
        SpringWebSocketAcpTransport.WebSocketAcpHandler webSocketAcpHandler = springWebSocketAcpTransport.new WebSocketAcpHandler();
        registry.addHandler(webSocketAcpHandler, springWebSocketAcpTransport.getPath())
                .setHandshakeHandler(webSocketAcpHandler)
                .setAllowedOrigins("*");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
