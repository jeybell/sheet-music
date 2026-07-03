package com.jeybell.sheetmusic.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SafeUrlValidatorTest {

    private final SafeUrlValidator validator = new SafeUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://youtu.be/abc123",
            "HTTPS://EXAMPLE.COM/path",
            "  https://example.com  "
    })
    void allowsHttpAndHttps(String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void allowsNullOrBlankBecauseOptional(String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "JavaScript:alert(document.cookie)",
            "  javascript:alert(1)",
            "data:text/html;base64,PHNjcmlwdD4=",
            "vbscript:msgbox(1)",
            "file:///etc/passwd",
            "ftp://example.com",
            "//evil.com",
            "example.com"
    })
    void rejectsUnsafeOrNonHttpSchemes(String url) {
        assertThat(validator.isValid(url, null)).isFalse();
    }

    @Test
    void rejectsTabObfuscatedJavascriptScheme() {
        // 브라우저는 스킴 내 제어문자를 무시하지만, 우리는 http(s) 접두사만 허용하므로 거부된다.
        assertThat(validator.isValid("java\tscript:alert(1)", null)).isFalse();
    }
}
