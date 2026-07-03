package com.jeybell.sheetmusic.admin;

import com.jeybell.sheetmusic.admin.dto.AdminUserResponse;
import com.jeybell.sheetmusic.auth.User;
import com.jeybell.sheetmusic.auth.UserRepository;
import com.jeybell.sheetmusic.auth.UserRole;
import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 사용자 관리. 권한 지정/해제와 계정 삭제를 다룬다.
 * 관리자가 실수로 스스로를 강등/삭제해 관리자 접근을 잃는 것을 막기 위해 본인 대상 작업은 거부한다.
 */
@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "userId")).stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse changeRole(Long userId, UserRole role, String currentUsername) {
        User user = getUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("자신의 권한은 변경할 수 없습니다.");
        }
        user.changeRole(role);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId, String currentUsername) {
        User user = getUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("자신의 계정은 삭제할 수 없습니다.");
        }
        userRepository.delete(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }
}
