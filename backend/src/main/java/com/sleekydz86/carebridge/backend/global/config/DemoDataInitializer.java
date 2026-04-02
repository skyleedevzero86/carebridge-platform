package com.sleekydz86.carebridge.backend.global.config;


import java.time.LocalDateTime;
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
        if (userJpaRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        userJpaRepository.save(new UserEntity(
                null,
                "admin",
                "운영관리자",
                passwordEncoder.encode("Admin1234!"),
                UserRole.ADMIN,
                now
        ));

        userJpaRepository.save(new UserEntity(
                null,
                "operator",
                "장비운영자",
                passwordEncoder.encode("Operator1234!"),
                UserRole.OPERATOR,
                now
        ));
    }
}