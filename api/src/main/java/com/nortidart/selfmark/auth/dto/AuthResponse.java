package com.nortidart.selfmark.auth.dto;

public record AuthResponse(String token, UserResponse user) { }
