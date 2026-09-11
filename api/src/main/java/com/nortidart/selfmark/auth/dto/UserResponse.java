package com.nortidart.selfmark.auth.dto;

import com.nortidart.selfmark.auth.entity.User;

public record UserResponse(Long id, String username, String nickname) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
