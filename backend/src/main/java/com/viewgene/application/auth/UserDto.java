package com.viewgene.application.auth;

import com.viewgene.domain.model.AppUser;
import com.viewgene.infrastructure.security.AuthenticatedUser;

public record UserDto(long id, String username, String displayName) {
    public static UserDto from(AppUser user) {
        return new UserDto(user.id(), user.username(), user.displayName());
    }

    public static UserDto from(AuthenticatedUser user) {
        return new UserDto(user.id(), user.username(), user.displayName());
    }
}
