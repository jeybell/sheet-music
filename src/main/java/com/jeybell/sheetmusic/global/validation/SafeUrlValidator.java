package com.jeybell.sheetmusic.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;

public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 선택 필드(null/blank)는 통과. 필수 여부는 @NotBlank 로 별도 검증한다.
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}
