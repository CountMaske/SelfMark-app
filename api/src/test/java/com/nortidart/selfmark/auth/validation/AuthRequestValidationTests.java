package com.nortidart.selfmark.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nortidart.selfmark.auth.dto.LoginRequest;
import com.nortidart.selfmark.auth.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AuthRequestValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsMainlandMobileAccount() {
        assertThat(validator.validate(new RegisterRequest("13800138000", "Password123", "小明"))).isEmpty();
        assertThat(validator.validate(new LoginRequest("13800138000", "Password123"))).isEmpty();
    }

    @Test
    void rejectsInvalidMobileAndMissingUsername() {
        var violations = validator.validate(new RegisterRequest("not-a-mobile", "Password123", ""));
        assertThat(violations).extracting(violation -> violation.getMessage())
                .contains("手机号格式错误", "username 不能为空");
    }

    @Test
    void rejectsPasswordOverBcryptUtf8Limit() {
        var violations = validator.validate(new LoginRequest("13800138000", "密".repeat(25)));
        assertThat(violations).extracting(violation -> violation.getMessage())
                .contains("password 不能超过 72 个 UTF-8 字节");
    }
}
