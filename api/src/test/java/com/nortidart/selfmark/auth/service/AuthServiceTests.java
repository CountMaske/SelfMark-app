package com.nortidart.selfmark.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nortidart.selfmark.auth.dto.LoginRequest;
import com.nortidart.selfmark.auth.dto.RegisterRequest;
import com.nortidart.selfmark.auth.dto.UserResponse;
import com.nortidart.selfmark.auth.entity.User;
import com.nortidart.selfmark.auth.mapper.UserMapper;
import com.nortidart.selfmark.auth.security.JwtUtil;
import com.nortidart.selfmark.common.exception.BusinessException;
import com.nortidart.selfmark.config.JwtProperties;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;

class AuthServiceTests {
    private UserMapper userMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        authService = new AuthService(userMapper, new BCryptPasswordEncoder(),
                new JwtUtil(new JwtProperties("selfmark-test-jwt-secret-at-least-32-characters", Duration.ofDays(7))));
    }

    @Test
    void registerStoresBcryptHashAndReturnsPasswordFreeDto() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        AtomicReference<User> inserted = new AtomicReference<>();
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            inserted.set(user);
            return 1;
        });

        var response = authService.register(new RegisterRequest("alice", "secret123", "Alice"));

        assertThat(inserted.get().getPassword()).startsWith("$2").isNotEqualTo("secret123");
        assertThat(new BCryptPasswordEncoder().matches("secret123", inserted.get().getPassword())).isTrue();
        assertThat(response.user()).isEqualTo(new UserResponse(7L, "alice", "Alice"));
        assertThat(response.token()).isNotBlank();
    }

    @Test
    void duplicateUsernameReturns409() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "secret123", null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(409);
                    assertThat(exception.getMessage()).isEqualTo("用户名已存在");
                });
    }

    @Test
    void concurrentDuplicateKeyIsMappedTo409() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("uk_user_username"))
                .when(userMapper).insert(any(User.class));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "secret123", null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(409);
                    assertThat(exception.getMessage()).isEqualTo("用户名已存在");
                });
    }

    @Test
    void registeredPasswordCanLogIn() {
        User user = user(7L, "alice", new BCryptPasswordEncoder().encode("secret123"));
        when(userMapper.selectOne(any())).thenReturn(user);
        assertThat(authService.login(new LoginRequest("alice", "secret123")).user().id()).isEqualTo(7L);
    }

    @Test
    void unknownUsernameAndWrongPasswordBothReturn401() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertUnauthorized(() -> authService.login(new LoginRequest("missing", "secret123")));

        when(userMapper.selectOne(any()))
                .thenReturn(user(7L, "alice", new BCryptPasswordEncoder().encode("right-password")));
        assertUnauthorized(() -> authService.login(new LoginRequest("alice", "wrong-password")));
        verify(userMapper, org.mockito.Mockito.times(2)).selectOne(any());
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(401));
    }

    private User user(Long id, String username, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
