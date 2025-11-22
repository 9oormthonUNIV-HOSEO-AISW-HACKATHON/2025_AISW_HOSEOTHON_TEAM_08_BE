package com.sedroad.service;

import com.sedroad.config.JwtUtil;
import com.sedroad.dto.AuthRequest;
import com.sedroad.dto.AuthResponse;
import com.sedroad.dto.UserDto;
import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 등록된 이메일입니다.");
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .generation(request.getGeneration())
                .build();

        user = userRepository.save(user);

        // 기본 프로필 생성
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
        User user = userRepository.findByEmail(request.getEmail())
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

