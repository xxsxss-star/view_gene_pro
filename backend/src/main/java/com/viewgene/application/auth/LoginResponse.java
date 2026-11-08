package com.viewgene.application.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserDto user
) {
}
