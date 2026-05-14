package com.sleekydz86.carebridge.backend.global.config;

import com.sleekydz86.carebridge.backend.server.interfaces.websocket.ChatWebSocketHandler;
import com.sleekydz86.carebridge.backend.server.interfaces.websocket.WebSocketHandshakeAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketHandshakeAuthInterceptor handshakeAuthInterceptor;
    private final AppProperties appProperties;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            WebSocketHandshakeAuthInterceptor handshakeAuthInterceptor,
            AppProperties appProperties
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeAuthInterceptor = handshakeAuthInterceptor;
        this.appProperties = appProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeAuthInterceptor)
                .setAllowedOrigins(appProperties.cors().allowedOrigins().toArray(String[]::new));
    }
}