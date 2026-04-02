package com.sleekydz86.carebridge.backend.global.config;

import java.time.LocalDateTime;
import com.sleekydz86.carebridge.backend.server.application.auth.UserEntity;
import com.sleekydz86.carebridge.backend.server.application.auth.UserJpaRepository;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserJpaRepository userJpaRepository, PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        ensureUser("admin", "운영관리자", "Admin1234!", UserRole.ADMIN, now);
        ensureUser("operator", "장비운영자", "Operator1234!", UserRole.OPERATOR, now);
    }

    private void ensureUser(String username, String displayName, String password, UserRole role, LocalDateTime createdAt) {
        UserEntity entity = userJpaRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            userJpaRepository.save(new UserEntity(
                    null,
                    username,
                    displayName,
                    passwordEncoder.encode(password),
                    role,
                    createdAt
            ));
            return;
        }

        userJpaRepository.save(new UserEntity(
                entity.getId(),
                entity.getUsername(),
                displayName,
                passwordEncoder.encode(password),
                role,
                entity.getCreatedAt()
        ));
    }
}