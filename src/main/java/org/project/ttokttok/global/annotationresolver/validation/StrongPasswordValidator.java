package org.project.ttokttok.global.annotationresolver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.project.ttokttok.global.annotation.validation.StrongPassword;
import org.springframework.stereotype.Component;

@Component
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;

        return password.length() >= 8 &&
                password.length() <= 20 &&
                password.matches(".*[a-z].*") &&     // 소문자 포함
                password.matches(".*[A-Z].*") &&     // 대문자 포함
                password.matches(".*\\d.*") &&       // 숫자 포함
                password.matches(".*[@$!%*?&].*");   // 특수문자 포함
    }
}
