package com.agentclientprotocol.sdk.spec;

/**
 *
 * @author Bugee
 */
public interface AcpSessionExt extends AcpSession {

    void putNotificationHandler(String toolCallId, AcpClientSession.NotificationHandler handler);

    void removeNotification(String toolCallId);
}
