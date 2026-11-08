package com.viewgene.infrastructure.security;

import com.viewgene.domain.model.AppUser;
import com.viewgene.domain.model.UserStatus;
import com.viewgene.infrastructure.persistence.mapper.AppUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;
    private final AppUserMapper appUserMapper;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(
            JwtTokenService tokenService,
            AppUserMapper appUserMapper,
            SecurityErrorWriter errorWriter
    ) {
        this.tokenService = tokenService;
        this.appUserMapper = appUserMapper;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "登录凭证无效");
            return;
        }

        try {
            JwtTokenService.TokenClaims claims = tokenService.parse(authorization.substring(BEARER_PREFIX.length()));
            Optional<AppUser> storedUser = appUserMapper.findById(claims.userId());
            if (storedUser.isEmpty() || !storedUser.get().username().equals(claims.username())) {
                errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "登录凭证无效");
                return;
            }
            AppUser user = storedUser.get();
            if (user.status() == UserStatus.DISABLED) {
                errorWriter.write(response, HttpStatus.FORBIDDEN.value(), "USER_DISABLED", "用户已被禁用");
                return;
            }
            AuthenticatedUser principal = new AuthenticatedUser(user.id(), user.username(), user.displayName());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, List.of())
            );
            chain.doFilter(request, response);
        } catch (JwtTokenService.TokenExpiredException exception) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "TOKEN_EXPIRED", "登录已过期");
        } catch (JwtTokenService.InvalidTokenException exception) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "登录凭证无效");
        }
    }
}
