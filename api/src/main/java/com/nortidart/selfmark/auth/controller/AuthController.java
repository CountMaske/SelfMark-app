package com.nortidart.selfmark.auth.controller;

import com.nortidart.selfmark.auth.dto.AuthResponse;
import com.nortidart.selfmark.auth.dto.LoginRequest;
import com.nortidart.selfmark.auth.dto.RegisterRequest;
import com.nortidart.selfmark.auth.service.AuthService;
import com.nortidart.selfmark.auth.service.TokenBlacklistService;
import com.nortidart.selfmark.auth.security.JwtUtil;
import com.nortidart.selfmark.common.context.UserContext;
import com.nortidart.selfmark.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        var currentUser = UserContext.getRequired();
        tokenBlacklistService.blacklist(currentUser.jti(), jwtUtil.getRemainingTtl(currentUser.token()));
        return ApiResponse.success();
    }
}
