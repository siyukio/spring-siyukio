package com.agentclientprotocol.sdk.agent;

import com.agentclientprotocol.sdk.spec.AcpAgentTransport;
import com.agentclientprotocol.sdk.util.Assert;

import java.time.Duration;

/**
 *
 * @author Bugee
 */
public interface SimpleAcpAgent {

    /**
     * Start building an asynchronous ACP agent with the specified transport layer.
     * The asynchronous agent provides non-blocking operations with Mono/Flux responses.
     *
     * @param transport The transport layer to use for communication
     * @return A builder for configuring the asynchronous agent
     */
    static SimpleAsyncAgentBuilder async(AcpAgentTransport transport) {
        return new SimpleAsyncAgentBuilder(transport);
    }

    /**
     * Builder for creating asynchronous ACP agents.
     */
    class SimpleAsyncAgentBuilder {

        private final AcpAgentTransport transport;

        private Duration requestTimeout = AcpAgent.DEFAULT_REQUEST_TIMEOUT;

        private AcpAgent.InitializeHandler initializeHandler;

        private AcpAgent.AuthenticateHandler authenticateHandler;

        private AcpAgent.NewSessionHandler newSessionHandler;

        private AcpAgent.LoadSessionHandler loadSessionHandler;

        private AcpAgent.PromptHandler promptHandler;

        private AcpAgent.SetSessionModeHandler setSessionModeHandler;

        private AcpAgent.SetSessionModelHandler setSessionModelHandler;

        private AcpAgent.CancelHandler cancelHandler;

        SimpleAsyncAgentBuilder(AcpAgentTransport transport) {
            Assert.notNull(transport, "Transport must not be null");
            this.transport = transport;
        }

        /**
         * Sets the timeout for requests sent to the client.
         *
         * @param timeout The request timeout duration
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder requestTimeout(Duration timeout) {
            Assert.notNull(timeout, "Timeout must not be null");
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * Sets the handler for initialize requests.
         *
         * @param handler The initialize handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder initializeHandler(AcpAgent.InitializeHandler handler) {
            this.initializeHandler = handler;
            return this;
        }

        /**
         * Sets the handler for authenticate requests.
         *
         * @param handler The authenticate handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder authenticateHandler(AcpAgent.AuthenticateHandler handler) {
            this.authenticateHandler = handler;
            return this;
        }

        /**
         * Sets the handler for new session requests.
         *
         * @param handler The new session handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder newSessionHandler(AcpAgent.NewSessionHandler handler) {
            this.newSessionHandler = handler;
            return this;
        }

        /**
         * Sets the handler for load session requests.
         *
         * @param handler The load session handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder loadSessionHandler(AcpAgent.LoadSessionHandler handler) {
            this.loadSessionHandler = handler;
            return this;
        }

        /**
         * Sets the handler for prompt requests.
         *
         * @param handler The prompt handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder promptHandler(AcpAgent.PromptHandler handler) {
            this.promptHandler = handler;
            return this;
        }

        /**
         * Sets the handler for set session mode requests.
         *
         * @param handler The set session mode handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder setSessionModeHandler(AcpAgent.SetSessionModeHandler handler) {
            this.setSessionModeHandler = handler;
            return this;
        }

        /**
         * Sets the handler for set session model requests.
         *
         * @param handler The set session model handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder setSessionModelHandler(AcpAgent.SetSessionModelHandler handler) {
            this.setSessionModelHandler = handler;
            return this;
        }

        /**
         * Sets the handler for cancel notifications.
         *
         * @param handler The cancel handler
         * @return This builder for chaining
         */
        public SimpleAsyncAgentBuilder cancelHandler(AcpAgent.CancelHandler handler) {
            this.cancelHandler = handler;
            return this;
        }

        /**
         * Builds the asynchronous ACP agent.
         *
         * @return A new AcpAsyncAgent instance
         */
        public AcpAsyncAgent build() {
            return new SimpleAcpAsyncAgent(transport, requestTimeout, initializeHandler, authenticateHandler,
                    newSessionHandler, loadSessionHandler, promptHandler, setSessionModeHandler, setSessionModelHandler,
                    cancelHandler);
        }

    }
}
