package com.viewgene.infrastructure.security;

import com.viewgene.config.JwtProperties;
import com.viewgene.domain.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    @Autowired
    public JwtTokenService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(AppUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(properties.expirationSeconds());
        String token = Jwts.builder()
                .subject(user.username())
                .claim("userId", user.id())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, properties.expirationSeconds());
    }

    public TokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token).getPayload();
            Number userId = claims.get("userId", Number.class);
            String username = claims.getSubject();
            if (userId == null || username == null || username.isBlank()) {
                throw new InvalidTokenException();
            }
            return new TokenClaims(userId.longValue(), username);
        } catch (ExpiredJwtException exception) {
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException();
        }
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }

    public record TokenClaims(long userId, String username) {
    }

    public static final class TokenExpiredException extends RuntimeException {
    }

    public static final class InvalidTokenException extends RuntimeException {
    }
}
