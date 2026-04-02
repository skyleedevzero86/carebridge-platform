package com.sleekydz86.carebridge.backend.server.interfaces.reset;

import com.sleekydz86.carebridge.backend.server.application.chat.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}