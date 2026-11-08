package com.viewgene.domain.model;

import java.time.LocalDateTime;

public record AppUser(
        Long id,
        String username,
        String displayName,
        String passwordHash,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
