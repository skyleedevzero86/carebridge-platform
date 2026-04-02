package com.sleekydz86.carebridge.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.sleekydz86.carebridge.backend.global.config.AppProperties;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserAccount;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AccessTokenService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public AccessTokenService(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public String issue(UserAccount account) {
        try {
            String header = ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            Instant expiresAt = Instant.now().plusSeconds(appProperties.security().tokenExpirationMinutes() * 60L);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", account.id().toString());
            payload.put("username", account.username());
            payload.put("displayName", account.displayName());
            payload.put("role", account.role().name());
            payload.put("exp", expiresAt.getEpochSecond());

            String encodedPayload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = header + "." + encodedPayload;
            String signature = sign(unsignedToken);
            return unsignedToken + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("토큰 발급에 실패했습니다.", exception);
        }
    }


    public String reissue(String expiredOrNearlyExpiredToken) {

        if (expiredOrNearlyExpiredToken == null || expiredOrNearlyExpiredToken.isBlank()) {
            throw unauthorized();
        }

        String[] parts = expiredOrNearlyExpiredToken.split("\\.");
        if (parts.length != 3) {
            throw unauthorized();
        }

        try {

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)
            )) {
                throw unauthorized();
            }

            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            Instant expiresAt = Instant.ofEpochSecond(exp);
            long thresholdMinutes = appProperties.security().tokenRefreshThresholdMinutes();


            if (Instant.now().isBefore(expiresAt.minusSeconds(thresholdMinutes * 60L))) {
                return expiredOrNearlyExpiredToken;
            }


            UserAccount account = new UserAccount(
                    UUID.fromString(String.valueOf(payload.get("sub"))),
                    String.valueOf(payload.get("username")),
                    String.valueOf(payload.get("displayName")),
                    "",
                    UserRole.valueOf(String.valueOf(payload.get("role"))),
                    null
            );

            return issue(account);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unauthorized();
        }
    }

    public AuthenticatedUserPrincipal parse(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw unauthorized();
        }

        try {
            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);


            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw unauthorized();
            }

            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});

            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (Instant.now().isAfter(Instant.ofEpochSecond(expiresAt))) {
                throw unauthorized();
            }

            return new AuthenticatedUserPrincipal(
                    UUID.fromString(String.valueOf(payload.get("sub"))),
                    String.valueOf(payload.get("username")),
                    String.valueOf(payload.get("displayName")),
                    UserRole.valueOf(String.valueOf(payload.get("role")))
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(appProperties.security().tokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다.");
    }
}