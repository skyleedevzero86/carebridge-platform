package com.sleekydz86.carebridge.backend.server.interfaces.reset;

import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import com.sleekydz86.carebridge.backend.server.application.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/presence")
    public ResponseEntity<List<AuthService.UserView>> presence() {
        return ResponseEntity.ok(authService.listMembers());
    }

    @PostMapping("/presence/ping")
    public ResponseEntity<Void> ping(Authentication authentication) {
        authService.refreshPresence((AuthenticatedUserPrincipal) authentication.getPrincipal());
        return ResponseEntity.ok().build();
    }
}