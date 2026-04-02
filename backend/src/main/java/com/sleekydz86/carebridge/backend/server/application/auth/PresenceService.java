package com.sleekydz86.carebridge.backend.server.application.auth;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    private static final Duration PRESENCE_TTL = Duration.ofSeconds(70);
    private static final String PRESENCE_PREFIX = "carebridge:presence:";

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markOnline(UUID userId) {
        redisTemplate.opsForValue().set(key(userId), "1", PRESENCE_TTL);
    }

    public void refresh(UUID userId) {
        markOnline(userId);
    }

    public void markOffline(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    public boolean isOnline(UUID userId) {
        Boolean exists = redisTemplate.hasKey(key(userId));
        return Boolean.TRUE.equals(exists);
    }

    private String key(UUID userId) {
        return PRESENCE_PREFIX + userId;
    }
}