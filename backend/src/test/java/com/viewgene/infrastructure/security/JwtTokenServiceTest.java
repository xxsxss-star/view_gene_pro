package com.viewgene.infrastructure.security;

import com.viewgene.config.JwtProperties;
import com.viewgene.domain.model.AppUser;
import com.viewgene.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {
    @Test
    void expiredTokenHasASeparateFailureType() {
        JwtProperties properties = new JwtProperties("stage-three-test-secret-must-be-at-least-32-bytes", 60);
        Clock issuedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        AppUser user = new AppUser(1L, "demo_user", "Demo", "unused", UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        String token = new JwtTokenService(properties, issuedClock).issue(user).value();

        Clock afterExpiry = Clock.fixed(Instant.parse("2026-01-01T00:02:00Z"), ZoneOffset.UTC);
        JwtTokenService parser = new JwtTokenService(properties, afterExpiry);
        assertThatThrownBy(() -> parser.parse(token))
                .isInstanceOf(JwtTokenService.TokenExpiredException.class);
    }
}
