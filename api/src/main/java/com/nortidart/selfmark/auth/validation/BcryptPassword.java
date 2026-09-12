package com.nortidart.selfmark.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BcryptPasswordValidator.class)
public @interface BcryptPassword {
    String message() default "password 不能超过 72 个 UTF-8 字节";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
