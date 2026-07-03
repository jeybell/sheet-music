package com.jeybell.sheetmusic.auth;

import com.jeybell.sheetmusic.auth.dto.AuthResponse;
import com.jeybell.sheetmusic.auth.dto.LoginRequest;
import com.jeybell.sheetmusic.auth.dto.RegisterRequest;
import com.jeybell.sheetmusic.global.exception.DuplicateUsernameException;
import com.jeybell.sheetmusic.global.exception.InvalidCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        User user = new User(request.username(), passwordEncoder.encode(request.password()));
        try {
            // existsByUsername 체크와 save 사이의 경쟁 상태 방어: 동시 요청으로 유니크 제약을
            // 위반하면 500 대신 명확한 409(DuplicateUsername)로 변환한다.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUsernameException(request.username());
        }
        return new AuthResponse(jwtService.generateToken(user.getUsername()), user.getUsername(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponse(jwtService.generateToken(user.getUsername()), user.getUsername(), user.getRole().name());
    }

    /**
     * 가입 없이 바로 사용해볼 수 있는 게스트 로그인. 현재 데이터는 팀 전체가 공유하고
     * 사용자별로 분리되지 않으므로(로그인은 접근 게이트 용도), 실제 계정 없이
     * 고정된 게스트 신원으로 토큰만 발급한다.
     */
    @Transactional(readOnly = true)
    public AuthResponse guestLogin() {
        return new AuthResponse(jwtService.generateToken("guest"), "guest", UserRole.USER.name());
    }
}
