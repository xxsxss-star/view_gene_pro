package com.viewgene;

import com.viewgene.domain.model.AppUser;
import com.viewgene.domain.model.UserStatus;
import com.viewgene.infrastructure.persistence.mapper.AppUserMapper;
import com.viewgene.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "app.persistence.enabled=false",
        "app.jwt.secret=stage-three-test-secret-must-be-at-least-32-bytes",
        "app.jwt.expiration-seconds=28800"
})
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService tokenService;
    @MockitoBean AppUserMapper appUserMapper;

    private AppUser activeUser;

    @BeforeEach
    void setUp() {
        activeUser = user(1, "demo_user", "演示用户", "demo123", UserStatus.ACTIVE);
        AppUser secondUser = user(2, "demo_user_b", "演示用户B", "demo123", UserStatus.ACTIVE);
        when(appUserMapper.findByUsername("demo_user")).thenReturn(Optional.of(activeUser));
        when(appUserMapper.findByUsername("demo_user_b")).thenReturn(Optional.of(secondUser));
        when(appUserMapper.findByUsername("missing")).thenReturn(Optional.empty());
        when(appUserMapper.findById(1)).thenReturn(Optional.of(activeUser));
        when(appUserMapper.findById(2)).thenReturn(Optional.of(secondUser));
        when(appUserMapper.findById(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            if (id == 1) return Optional.of(activeUser);
            if (id == 2) return Optional.of(secondUser);
            return Optional.empty();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo_user", "demo_user_b"})
    void presetUsersCanLogin(String username) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"demo123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(28800))
                .andExpect(jsonPath("$.data.user.username").value(username));
    }

    @Test
    void wrongPasswordAndUnknownUserUseSameErrorCode() throws Exception {
        assertInvalidCredentials("demo_user", "wrong");
        assertInvalidCredentials("missing", "demo123");
    }

    @Test
    void disabledUserCannotLoginOrReuseAnExistingToken() throws Exception {
        AppUser disabled = user(3, "disabled_user", "禁用用户", "demo123", UserStatus.DISABLED);
        when(appUserMapper.findByUsername("disabled_user")).thenReturn(Optional.of(disabled));
        when(appUserMapper.findById(3)).thenReturn(Optional.of(disabled));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"disabled_user\",\"password\":\"demo123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_DISABLED"));

        String token = tokenService.issue(disabled).value();
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_DISABLED"));
    }

    @Test
    void meRestoresUserAndLogoutIsStateless() throws Exception {
        String token = tokenService.issue(activeUser).value();
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("demo_user"));

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void protectedEndpointsRejectMissingAndTamperedTokens() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        String token = tokenService.issue(activeUser).value();
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    private void assertInvalidCredentials(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    private AppUser user(long id, String username, String displayName, String password, UserStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new AppUser(id, username, displayName, passwordEncoder.encode(password), status, now, now);
    }
}
