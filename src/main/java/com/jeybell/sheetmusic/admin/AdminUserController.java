package com.jeybell.sheetmusic.admin;

import com.jeybell.sheetmusic.admin.dto.AdminUserResponse;
import com.jeybell.sheetmusic.admin.dto.RoleUpdateRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 사용자 관리 API. 접근 인가는 SecurityConfig 의 {@code /api/admin/** → hasRole('ADMIN')} 규칙으로 보장.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> changeRole(
            @PathVariable("id") Long id,
            @Valid @RequestBody RoleUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminUserService.changeRole(id, request.role(), principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id, Principal principal) {
        adminUserService.deleteUser(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
