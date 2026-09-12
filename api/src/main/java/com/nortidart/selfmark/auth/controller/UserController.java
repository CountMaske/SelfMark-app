package com.nortidart.selfmark.auth.controller;

import com.nortidart.selfmark.auth.dto.UserResponse;
import com.nortidart.selfmark.auth.service.UserService;
import com.nortidart.selfmark.common.context.UserContext;
import com.nortidart.selfmark.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(userService.getCurrentUser(UserContext.getUserId()));
    }
}
