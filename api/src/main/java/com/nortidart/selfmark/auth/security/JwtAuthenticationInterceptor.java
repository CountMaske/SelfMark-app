package com.nortidart.selfmark.auth.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortidart.selfmark.common.context.CurrentUser;
import com.nortidart.selfmark.common.context.UserContext;
import com.nortidart.selfmark.common.response.ApiResponse;
import com.nortidart.selfmark.auth.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper,
            TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.length() <= "Bearer ".length()) {
            writeUnauthorized(response, "未登录");
            return false;
        }
        try {
            String token = authorization.substring("Bearer ".length()).trim();
            DecodedJWT jwt = jwtUtil.parse(token);
            Long userId = jwt.getClaim("userId").asLong();
            if (userId == null && jwt.getSubject() != null) {
                userId = Long.valueOf(jwt.getSubject());
            }
            if (userId == null) {
                writeUnauthorized(response, "token 无效");
                return false;
            }
            String jti = jwt.getId();
            if (jti == null || jti.isBlank()) {
                writeUnauthorized(response, "token 无效");
                return false;
            }
            if (tokenBlacklistService.isBlacklisted(jti)) {
                writeUnauthorized(response, "token 已登出");
                return false;
            }
            UserContext.set(new CurrentUser(userId, jwt.getClaim("role").asString(), jti, token));
            return true;
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            writeUnauthorized(response, "token 无效");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(401, message));
    }
}
