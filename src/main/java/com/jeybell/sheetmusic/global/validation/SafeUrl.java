package com.jeybell.sheetmusic.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 사용자가 입력한 URL이 안전한 스킴(http/https)인지 검증한다.
 * javascript:, data:, vbscript: 등으로 인한 저장형 XSS 를 서버 단에서 차단한다.
 * null/blank 는 허용하므로(선택 필드) 필수 여부는 별도로 @NotBlank 로 지정할 것.
 */
@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeUrl {

    String message() default "URL 은 http 또는 https 로 시작해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
