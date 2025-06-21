package io.modelcontextprotocol.client.transport;

import io.github.siyukio.tools.util.AsyncUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Bugee
 */
@Slf4j
public class MyWebSocket {

    private final WebSocket webSocket;

    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public MyWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
        AsyncUtils.submit(this::processQueue);
    }

    private void processQueue() {
        try {
            while (true) {
                String msg = messageQueue.take();
                this.webSocket.sendText(msg, true).join();
                log.debug("sendText {}", msg.length());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendText(String data) {
        this.messageQueue.offer(data);
    }

    public void sendClose(int statusCode, String reason) {
        this.webSocket.sendClose(statusCode, reason);
    }

}
