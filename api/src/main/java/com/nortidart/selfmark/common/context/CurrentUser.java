package com.nortidart.selfmark.common.context;

public record CurrentUser(Long userId, String role, String jti, String token) {
}
