package com.jeybell.sheetmusic.admin.dto;

import com.jeybell.sheetmusic.auth.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull(message = "role is required") UserRole role
) {
}
