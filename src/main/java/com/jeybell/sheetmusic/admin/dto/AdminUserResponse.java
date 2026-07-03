package com.jeybell.sheetmusic.admin.dto;

import com.jeybell.sheetmusic.auth.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String username,
        String role,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
