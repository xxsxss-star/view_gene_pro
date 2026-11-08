package com.viewgene.application.auth;

import com.viewgene.common.exception.BusinessException;
import com.viewgene.domain.model.AppUser;
import com.viewgene.domain.model.UserStatus;
import com.viewgene.infrastructure.persistence.mapper.AppUserMapper;
import com.viewgene.infrastructure.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthService(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        AppUser user = appUserMapper.findByUsername(username)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }
        if (user.status() == UserStatus.DISABLED) {
            throw new BusinessException("USER_DISABLED", "用户已被禁用", HttpStatus.FORBIDDEN);
        }
        JwtTokenService.IssuedToken token = tokenService.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), UserDto.from(user));
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
    }
}
