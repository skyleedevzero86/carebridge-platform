package com.sleekydz86.carebridge.backend.server.interfaces.websocket;


import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sleekydz86.carebridge.backend.global.security.AccessTokenService;
import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import com.sleekydz86.carebridge.backend.server.application.auth.AuthService;
import com.sleekydz86.carebridge.backend.server.application.chat.ChatService;
import com.sleekydz86.carebridge.backend.server.application.device.DeviceRealtimeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final AccessTokenService accessTokenService;
    private final AuthService authService;
    private final ChatService chatService;
    private final Map<String, SessionClient> clients = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            AccessTokenService accessTokenService,
            AuthService authService,
            ChatService chatService
    ) {
        this.objectMapper = objectMapper;
        this.accessTokenService = accessTokenService;
        this.authService = authService;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthenticatedUserPrincipal principal = authenticate(session.getUri());
        authService.markOnline(principal);

        ConcurrentWebSocketSessionDecorator safeSession = new ConcurrentWebSocketSessionDecorator(session, 10_000, 64 * 1024);
        clients.put(session.getId(), new SessionClient(principal, safeSession));

        sendEnvelope(safeSession, "CONNECTED", authService.me(principal));
        broadcastPresence();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionClient client = clients.get(session.getId());
        if (client == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        InboundMessage inboundMessage = objectMapper.readValue(message.getPayload(), InboundMessage.class);

        if ("PING".equalsIgnoreCase(inboundMessage.type())) {
            authService.refreshPresence(client.principal());
            sendEnvelope(client.session(), "PONG", Map.of("status", "ok"));
            return;
        }

        if ("CHAT".equalsIgnoreCase(inboundMessage.type())) {
            ChatService.ChatMessageView saved = chatService.append(client.principal(), inboundMessage.content());
            broadcast("CHAT_MESSAGE", saved);
            return;
        }

        sendEnvelope(client.session(), "ERROR", Map.of("message", "지원하지 않는 소켓 메시지 타입입니다."));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionClient client = clients.remove(session.getId());
        if (client != null) {
            authService.logout(client.principal());
            broadcastPresence();
        }

        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        SessionClient client = clients.remove(session.getId());
        if (client != null) {
            authService.logout(client.principal());
            broadcastPresence();
        }

        session.close(CloseStatus.SERVER_ERROR);
    }

    @EventListener
    public void onDeviceRealtimeEvent(DeviceRealtimeEvent event) {
        broadcast("DEVICE_EVENT", event.view());
    }

    private AuthenticatedUserPrincipal authenticate(URI uri) {
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소켓 인증 토큰이 없습니다.");
        }
        return accessTokenService.parse(token);
    }

    private void broadcastPresence() {
        broadcast("PRESENCE_SNAPSHOT", authService.listMembers());
    }

    private void broadcast(String type, Object payload) {
        clients.values().forEach(client -> {
            try {
                sendEnvelope(client.session(), type, payload);
            } catch (IllegalStateException ignored)  {}
        });
    }

    private void sendEnvelope(WebSocketSession session, String type, Object payload) {
        if (!session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new SocketEnvelope(type, payload))));
        } catch (IOException exception) {
            throw new IllegalStateException("웹소켓 메시지 전송에 실패했습니다.", exception);
        }
    }

    private record SessionClient(AuthenticatedUserPrincipal principal, WebSocketSession session)  {}

    private record InboundMessage(String type, String content)  {}

    private record SocketEnvelope(String type, Object payload)  {}
}