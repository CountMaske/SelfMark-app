package com.nortidart.selfmark.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BcryptPasswordValidatorTests {
    private final BcryptPasswordValidator validator = new BcryptPasswordValidator();

    @Test
    void acceptsAtMost72Utf8Bytes() {
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
        assertThat(validator.isValid("密".repeat(24), null)).isTrue();
    }

    @Test
    void rejectsMoreThan72Utf8Bytes() {
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
        assertThat(validator.isValid("密".repeat(25), null)).isFalse();
    }
}
