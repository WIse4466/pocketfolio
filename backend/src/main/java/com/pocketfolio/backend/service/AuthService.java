package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AuthResponse;
import com.pocketfolio.backend.dto.LoginRequest;
import com.pocketfolio.backend.dto.RegisterRequest;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.repository.UserRepository;
import com.pocketfolio.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // 註冊
    public AuthResponse register(RegisterRequest request) {

        // 檢查 email 是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email 已被註冊");
        }

        // 檢查 username 是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("使用者名稱已被使用");
        }

        // 建立新用戶
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // 產生 JWT Token
        String token = jwtUtil.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .build();
    }

    // 登入
    public AuthResponse login(LoginRequest request) {
        // Spring Security 驗證
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();

        // 更新最後登入時間
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 產生 JWT Token
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }
}
