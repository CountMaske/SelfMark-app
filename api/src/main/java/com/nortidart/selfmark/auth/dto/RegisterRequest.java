package com.nortidart.selfmark.auth.dto;

import com.nortidart.selfmark.auth.validation.BcryptPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username 不能为空")
        @Size(max = 50, message = "username 最长 50 个字符")
        String username,
        @NotBlank(message = "password 不能为空")
        @BcryptPassword
        String password,
        @Size(max = 50, message = "nickname 最长 50 个字符")
        String nickname) { }
