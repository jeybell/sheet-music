package com.jeybell.sheetmusic.auth;

import com.jeybell.sheetmusic.auth.dto.AuthResponse;
import com.jeybell.sheetmusic.auth.dto.LoginRequest;
import com.jeybell.sheetmusic.auth.dto.RegisterRequest;
import com.jeybell.sheetmusic.global.exception.DuplicateUsernameException;
import com.jeybell.sheetmusic.global.exception.InvalidCredentialsException;
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
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user.getUsername()), user.getUsername());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponse(jwtService.generateToken(user.getUsername()), user.getUsername());
    }
}
