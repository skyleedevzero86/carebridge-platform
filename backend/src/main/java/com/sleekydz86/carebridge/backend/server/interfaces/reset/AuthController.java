package com.sleekydz86.carebridge.backend.server.interfaces.reset;

import com.sleekydz86.carebridge.backend.global.security.AccessTokenService;
import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import com.sleekydz86.carebridge.backend.server.application.auth.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessTokenService accessTokenService;

    public AuthController(AuthService authService, AccessTokenService accessTokenService) {
        this.authService = authService;
        this.accessTokenService = accessTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthService.AuthResult> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request.username(), request.displayName(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.AuthResult> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthService.UserView> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(current(authentication)));
    }


    @PostMapping("/refresh")
    public ResponseEntity<RefreshResult> refresh(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String currentToken = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        String newToken = accessTokenService.reissue(currentToken);
        boolean refreshed = !newToken.equals(currentToken);
        return ResponseEntity.ok(new RefreshResult(newToken, refreshed));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        authService.logout(current(authentication));
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    private AuthenticatedUserPrincipal current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        return principal;
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30) String username,
            @NotBlank @Size(min = 2, max = 20) String displayName,
            @NotBlank @Size(min = 8, max = 40) String password
    ) {}
    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}
    public record RefreshResult(String accessToken, boolean refreshed)  {}
}