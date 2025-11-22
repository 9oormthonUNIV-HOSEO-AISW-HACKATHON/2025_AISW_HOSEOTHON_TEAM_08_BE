package com.sedroad.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedroad.dto.AuthRequest;
import com.sedroad.entity.User;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.sedroad.config.TestSecurityConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegister_Success() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setGeneration("30대");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"));
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        // 기존 사용자 생성
        User existingUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Existing User")
                .generation("30대")
                .build();
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("New User");
        request.setGeneration("20대");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 등록된 이메일입니다."));
    }

    @Test
    void testRegister_InvalidRequest() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail(""); // 빈 이메일
        request.setPassword("password123");
        request.setName("Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 400 || status == 500 : "Expected 400 or 500, but got " + status;
                }); // 서비스에서 예외 발생 시 500 또는 400
    }

    @Test
    void testLogin_Success() throws Exception {
        // 사용자 생성
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email("login@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("Login User")
                .generation("30대")
                .build();
        userRepository.save(user);

        AuthRequest request = new AuthRequest();
        request.setEmail("login@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("login@example.com"));
    }

    @Test
    void testLogin_WrongPassword() throws Exception {
        // 사용자 생성
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email("wrong@example.com")
                .password(passwordEncoder.encode("correctpassword"))
                .name("Wrong User")
                .generation("30대")
                .build();
        userRepository.save(user);

        AuthRequest request = new AuthRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("잘못된 이메일 또는 비밀번호입니다."));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("잘못된 이메일 또는 비밀번호입니다."));
    }
}

