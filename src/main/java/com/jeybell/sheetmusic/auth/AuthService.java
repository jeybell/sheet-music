package com.jeybell.sheetmusic.auth;

import com.jeybell.sheetmusic.auth.dto.AuthResponse;
import com.jeybell.sheetmusic.auth.dto.LoginRequest;
import com.jeybell.sheetmusic.auth.dto.RegisterRequest;
import com.jeybell.sheetmusic.global.exception.DuplicateUsernameException;
import com.jeybell.sheetmusic.global.exception.InvalidCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
     * DB를 전혀 조회하지 않으므로 클래스 레벨 @Transactional을 명시적으로 꺼서(NOT_SUPPORTED)
     * Hibernate가 read-only 트랜잭션 시작 시 커넥션 풀에서 물리 커넥션을 선점하지 않게 한다
     * (readOnly 트랜잭션도 시작 시점에 read-only 플래그를 걸기 위해 커넥션을 즉시 확보함).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthResponse guestLogin() {
        return new AuthResponse(jwtService.generateToken("guest"), "guest", UserRole.USER.name());
    }
}
