package com.nortidart.selfmark.auth.dto;

import com.nortidart.selfmark.auth.validation.BcryptPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "mobile 不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
        String mobile,
        @NotBlank(message = "password 不能为空")
        @BcryptPassword
        String password,
        @NotBlank(message = "username 不能为空")
        @Size(max = 50, message = "username 最长 50 个字符")
        String username) { }
