package io.github.siyukio.application.boot.starter.autoconfigure;


import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.AcpAsyncAgent;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.application.acp.transport.WebSocketAcpTransport;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.token.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import reactor.core.publisher.Mono;

/**
 * @author Bugee
 */
@Import(DelegatingWebSocketConfiguration.class)
@AutoConfigureAfter({SiyukioApplicationAutoConfiguration.class})
@Slf4j
public class SiyukioWebSocketAcpServerAutoConfiguration implements WebSocketConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public WebSocketAcpTransport webSocketAcpTransport() {
        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        return new WebSocketAcpTransport(tokenProvider);
    }

    @Bean
    public AcpAsyncAgent acpAsyncAgent() {
        WebSocketAcpTransport webSocketAcpTransport = this.applicationContext.getBean(WebSocketAcpTransport.class);
        AipHandlerManager aipHandlerManager = this.applicationContext.getBean(AipHandlerManager.class);
        AcpAgent.AsyncAgentBuilder builder = AcpAgent.async(webSocketAcpTransport);
        builder.initializeHandler((request) -> Mono.just(AcpSchema.InitializeResponse.ok()));
        builder.newSessionHandler(webSocketAcpTransport.new AcpNewSessionHandler());
        builder.promptHandler(webSocketAcpTransport.new AcpPromptHandler(aipHandlerManager.getApiHandlerMap()));
        AcpAsyncAgent acpAsyncAgent = builder.build();
        acpAsyncAgent.start().subscribe();
        log.info("Start WebSocket Acp server {}", acpAsyncAgent.getClientCapabilities());
        return acpAsyncAgent;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketAcpTransport webSocketAcpTransport = this.applicationContext.getBean(WebSocketAcpTransport.class);
        WebSocketAcpTransport.WebSocketAcpHandler webSocketAcpHandler = webSocketAcpTransport.new WebSocketAcpHandler();
        registry.addHandler(webSocketAcpHandler, webSocketAcpTransport.getPath())
                .setHandshakeHandler(webSocketAcpHandler)
                .setAllowedOrigins("*");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
