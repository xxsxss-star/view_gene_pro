package com.viewgene.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationSeconds) {
    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must contain at least 32 UTF-8 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("app.jwt.expiration-seconds must be positive");
        }
    }
}
