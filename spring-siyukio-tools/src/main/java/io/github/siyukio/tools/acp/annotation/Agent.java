package io.github.siyukio.tools.acp.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marks a class as an ACP (Agent Client Protocol) agent implementation.
 * Each annotated class represents a unique agent that handles ACP sessions.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Agent("chat-assistant")
 * public class ChatAssistantHandler implements AcpSessionHandler {
 *     // implementation...
 * }
 * }</pre>
 *
 * @author Bugee
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // auto-register as Spring bean
public @interface Agent {

    /**
     * The unique name of this agent.
     * Used to identify which agent should handle incoming ACP requests.
     * If empty, this agent will be treated as the default agent.
     */
    String value() default "";

    /**
     * Alias for {@link #value()} for better readability.
     */
    @AliasFor("value")
    String name() default "";

}
