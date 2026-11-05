package com.jobassistant.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Emissor e validador de tokens JWT assinados via HMAC-SHA256 (HS256),
 * garantindo integridade criptográfica da identidade do usuário sem confiar em cabeçalhos arbitrários.
 */
@Service
public class AuthTokenService {

    private static final String HMAC_ALG = "HmacSHA256";
    private final byte[] secretBytes;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public AuthTokenService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt-secret:}") String configuredSecret,
            @Value("${app.security.jwt-expiration-seconds:86400}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.expirationSeconds = Math.max(300, expirationSeconds);
        if (configuredSecret != null && configuredSecret.trim().length() >= 32) {
            this.secretBytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] randomKey = new byte[32];
            new SecureRandom().nextBytes(randomKey);
            this.secretBytes = randomKey;
        }
    }

    public String issueToken(UUID userId, String email) {
        try {
            long now = Instant.now().getEpochSecond();
            String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", userId.toString());
            claims.put("email", email != null ? email : "");
            claims.put("iat", now);
            claims.put("exp", now + expirationSeconds);
            String payloadJson = objectMapper.writeValueAsString(claims);

            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = base64UrlEncode(hmacSha256(signingInput));

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao emitir token de sessão autenticado.", e);
        }
    }

    public Optional<AuthenticatedUserPrincipal> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSig = hmacSha256(signingInput);
            byte[] providedSig = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSig, providedSig)) {
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(payloadBytes);
            long exp = claims.path("exp").asLong(0L);
            if (exp <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            UUID userId = UUID.fromString(claims.path("sub").asText());
            String email = claims.path("email").asText("");
            return Optional.of(new AuthenticatedUserPrincipal(userId, email, "JWT"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private byte[] hmacSha256(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALG);
        mac.init(new SecretKeySpec(secretBytes, HMAC_ALG));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
