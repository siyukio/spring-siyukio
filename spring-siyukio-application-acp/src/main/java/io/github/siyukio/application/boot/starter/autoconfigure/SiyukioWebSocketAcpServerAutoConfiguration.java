package io.github.siyukio.application.boot.starter.autoconfigure;


import com.agentclientprotocol.sdk.agent.AcpAsyncAgent;
import com.agentclientprotocol.sdk.agent.SimpleAcpAgent;
import com.agentclientprotocol.sdk.agent.transport.SpringWebSocketAcpTransport;
import io.github.siyukio.application.acp.AcpSessionHandler;
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

/**
 * @author Bugee
 */
@Import(DelegatingWebSocketConfiguration.class)
@AutoConfigureAfter({SiyukioApplicationAutoConfiguration.class})
@Slf4j
public class SiyukioWebSocketAcpServerAutoConfiguration implements WebSocketConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public SpringWebSocketAcpTransport webSocketAcpTransport(ObjectProvider<AcpSessionHandler> acpSessionHandlerProvider) {
        AcpSessionHandler acpSessionHandler = acpSessionHandlerProvider.getIfAvailable(() -> {
            throw new IllegalStateException("""
                    AcpSessionHandler not found, please implement AcpSessionHandler and register it as a bean.
                    
                    Example implementation:
                    
                        @Slf4j
                        @Service
                        public class AcpSessionHandlerImpl implements AcpSessionHandler {
                    
                            @Override
                            public AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
                                log.debug("AcpSchema.InitializeRequest: {}, {}", token, req);
                                return AcpSessionHandler.super.init(token, req);
                            }
                    
                            @Override
                            public AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
                                log.debug("AcpSchema.NewSessionResponse: {}, {}", token, req);
                                return AcpSessionHandler.super.newSession(token, req);
                            }
                        }
                    """);
        });
        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        return new SpringWebSocketAcpTransport(tokenProvider, acpSessionHandler);
    }

    @Bean
    public AcpAsyncAgent acpAsyncAgent() {
        SpringWebSocketAcpTransport springWebSocketAcpTransport = this.applicationContext.getBean(SpringWebSocketAcpTransport.class);
        AipHandlerManager aipHandlerManager = this.applicationContext.getBean(AipHandlerManager.class);
        SimpleAcpAgent.SimpleAsyncAgentBuilder builder = SimpleAcpAgent.async(springWebSocketAcpTransport)
                .initializeHandler(springWebSocketAcpTransport.new AcpInitializeHandler())
                .newSessionHandler(springWebSocketAcpTransport.new AcpNewSessionHandler())
                .loadSessionHandler(springWebSocketAcpTransport.new AcpLoadSessionHandler())
                .promptHandler(springWebSocketAcpTransport.new AcpPromptHandler(aipHandlerManager.getApiHandlerMap()));
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
