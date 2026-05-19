package com.finportfolio.service;

import com.finportfolio.dto.AuthResponse;
import com.finportfolio.dto.LoginRequest;
import com.finportfolio.dto.RegisterRequest;
import com.finportfolio.entity.RefreshToken;
import com.finportfolio.entity.User;
import com.finportfolio.exception.BusinessException;
import com.finportfolio.exception.ResourceNotFoundException;
import com.finportfolio.repository.RefreshTokenRepository;
import com.finportfolio.repository.UserRepository;
import com.finportfolio.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.email())) {
            auditService.log(null, "REGISTER", false, "Email zaten kayitli: " + request.email(), httpRequest);
            throw new BusinessException("Bu email zaten kayitli", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .isActive(true)
                .build();
        @SuppressWarnings("null")
        User savedUser = userRepository.save(user);
        user = savedUser;

        auditService.log(user.getId(), "REGISTER", true, "Yeni kullanici kaydi", httpRequest);

        return generateTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
            );
        } catch (Exception e) {
            auditService.log(null, "LOGIN", false, "Hatali giris: " + request.email(), httpRequest);
            throw e;
        }

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici bulunamadi"));

        user.setLastLoginAt(Instant.now());
        @SuppressWarnings({"null", "unused"})
        User updatedUser = userRepository.save(user);

        auditService.log(user.getId(), "LOGIN", true, "Basarili giris", httpRequest);

        return generateTokens(user);
    }

    private AuthResponse generateTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getEmail(), user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        @SuppressWarnings({"null", "unused"})
        var savedToken = refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }
}