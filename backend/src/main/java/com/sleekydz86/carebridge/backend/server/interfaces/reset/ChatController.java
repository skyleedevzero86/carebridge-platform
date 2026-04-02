package com.sleekydz86.carebridge.backend.server.interfaces.reset;

import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import com.sleekydz86.carebridge.backend.server.application.chat.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    @GetMapping("/messages")
    public ResponseEntity<List<ChatService.ChatMessageView>> recentMessages(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(chatService.recentMessages(page));
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatService.ChatMessageView> sendMessage(
            Authentication authentication,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(chatService.append(current(authentication), request.content()));
    }

    private AuthenticatedUserPrincipal current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        return principal;
    }

    public record SendMessageRequest(@NotBlank String content) {}
}