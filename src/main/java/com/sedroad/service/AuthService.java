package com.sedroad.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sedroad.config.JwtUtil;
import com.sedroad.dto.AuthRequest;
import com.sedroad.dto.AuthResponse;
import com.sedroad.dto.UserDto;
import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 정보가 필요합니다.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("이메일이 필요합니다.");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("비밀번호가 필요합니다.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("이름이 필요합니다.");
        }
        
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new RuntimeException("이미 등록된 이메일입니다.");
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .generation(request.getGeneration() != null ? request.getGeneration().trim() : null)
                .build();

        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .build();
        userProfileRepository.save(profile);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .success(true)
                .message("회원가입이 완료되었습니다.")
                .token(token)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .generation(user.getGeneration())
                        .build())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 정보가 필요합니다.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("이메일이 필요합니다.");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("비밀번호가 필요합니다.");
        }
        
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("잘못된 이메일 또는 비밀번호입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("잘못된 이메일 또는 비밀번호입니다.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .success(true)
                .message("로그인 성공")
                .token(token)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .generation(user.getGeneration())
                        .build())
                .build();
    }
}

