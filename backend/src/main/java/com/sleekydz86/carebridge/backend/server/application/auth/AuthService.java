package com.sleekydz86.carebridge.backend.server.application.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import com.sleekydz86.carebridge.backend.global.security.AccessTokenService;
import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserAccount;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserAccountFactory;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final PresenceService presenceService;

    public AuthService(
            UserJpaRepository userJpaRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            PresenceService presenceService
    ) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.presenceService = presenceService;
    }

    public AuthResult register(String username, String displayName, String password) {
        validatePassword(password);
        String normalizedUsername = normalizeUsername(username);

        if (userJpaRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }

        UserAccount account = UserAccountFactory.create(
                normalizedUsername,
                displayName,
                passwordEncoder.encode(password),
                UserRole.OPERATOR,
                LocalDateTime.now()
        );

        UserEntity saved = userJpaRepository.save(toEntity(account));
        presenceService.markOnline(saved.getId());
        return issueResult(toDomain(saved));
    }

    @Transactional(readOnly = true)
    public AuthResult login(String username, String password) {
        UserEntity entity = userJpaRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

        if (!passwordEncoder.matches(password, entity.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 아이디 또는 비밀번호입니다.");
        }

        presenceService.markOnline(entity.getId());
        return issueResult(toDomain(entity));
    }

    @Transactional(readOnly = true)
    public UserView me(AuthenticatedUserPrincipal principal) {
        UserEntity entity = userJpaRepository.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        return toView(toDomain(entity));
    }

    @Transactional(readOnly = true)
    public List<UserView> listMembers() {
        return userJpaRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::toDomain)
                .map(this::toView)
                .toList();
    }

    public void markOnline(AuthenticatedUserPrincipal principal) {
        presenceService.markOnline(principal.userId());
    }

    public void refreshPresence(AuthenticatedUserPrincipal principal) {
        presenceService.refresh(principal.userId());
    }

    public void logout(AuthenticatedUserPrincipal principal) {
        presenceService.markOffline(principal.userId());
    }

    private AuthResult issueResult(UserAccount account) {
        return new AuthResult(accessTokenService.issue(account), toView(account));
    }

    private UserView toView(UserAccount account) {
        return new UserView(
                account.id().toString(),
                account.username(),
                account.displayName(),
                account.role(),
                presenceService.isOnline(account.id()) ? 1 : 0
        );
    }

    private UserAccount toDomain(UserEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }

    private UserEntity toEntity(UserAccount account) {
        return new UserEntity(
                account.id(),
                account.username(),
                account.displayName(),
                account.passwordHash(),
                account.role(),
                account.createdAt()
        );
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디를 입력해주세요.");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 최소 8자 이상이어야 합니다.");
        }
    }

    public record AuthResult(String accessToken, UserView user)  {}

    public record UserView(String id, String username, String displayName, UserRole role, int online)  {}
}