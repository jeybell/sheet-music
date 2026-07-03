package com.jeybell.sheetmusic.auth;

/**
 * 사용자 권한. 기본값은 {@link #USER}, 운영 관리 기능은 {@link #ADMIN}만 접근한다.
 * Spring Security authority 는 "ROLE_" + name() 규칙으로 매핑된다(ROLE_USER / ROLE_ADMIN).
 */
public enum UserRole {
    USER,
    ADMIN
}
