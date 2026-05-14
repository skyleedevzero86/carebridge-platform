package com.sleekydz86.carebridge.backend.server.interfaces.websocket;

import java.util.Map;
import com.sleekydz86.carebridge.backend.global.security.AccessTokenService;
import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WebSocketHandshakeAuthInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTR = "carebridge.ws.principal";

    private final AccessTokenService accessTokenService;

    public WebSocketHandshakeAuthInterceptor(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            AuthenticatedUserPrincipal principal = accessTokenService.parse(token);
            attributes.put(PRINCIPAL_ATTR, principal);
            return true;
        } catch (Exception ignored) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
