package com.viewgene.infrastructure.security;

public record AuthenticatedUser(long id, String username, String displayName) {
}
