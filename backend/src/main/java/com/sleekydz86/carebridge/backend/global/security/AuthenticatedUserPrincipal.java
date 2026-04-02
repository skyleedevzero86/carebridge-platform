package com.sleekydz86.carebridge.backend.global.security;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUserPrincipal(
        UUID userId,
        String username,
        String displayName,
        UserRole role
) implements Principal {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return username;
    }
}