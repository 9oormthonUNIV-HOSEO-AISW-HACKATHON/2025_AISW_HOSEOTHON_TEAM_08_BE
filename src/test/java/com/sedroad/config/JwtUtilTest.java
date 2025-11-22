package com.sedroad.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken() {
        String userId = "test-user-id";
        String email = "test@example.com";
        
        String token = jwtUtil.generateToken(userId, email);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGenerateTokenWithNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.generateToken(null, "test@example.com");
        });
    }

    @Test
    void testGenerateTokenWithNullEmail() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.generateToken("test-user-id", null);
        });
    }

    @Test
    void testExtractUserId() {
        String userId = "test-user-id";
        String email = "test@example.com";
        String token = jwtUtil.generateToken(userId, email);
        
        String extractedUserId = jwtUtil.extractUserId(token);
        
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testExtractEmail() {
        String userId = "test-user-id";
        String email = "test@example.com";
        String token = jwtUtil.generateToken(userId, email);
        
        String extractedEmail = jwtUtil.extractEmail(token);
        
        assertEquals(email, extractedEmail);
    }

    @Test
    void testValidateToken() {
        String userId = "test-user-id";
        String email = "test@example.com";
        String token = jwtUtil.generateToken(userId, email);
        
        Boolean isValid = jwtUtil.validateToken(token, userId);
        
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithWrongUserId() {
        String userId = "test-user-id";
        String email = "test@example.com";
        String token = jwtUtil.generateToken(userId, email);
        
        Boolean isValid = jwtUtil.validateToken(token, "wrong-user-id");
        
        assertFalse(isValid);
    }

    @Test
    void testValidateTokenWithNullToken() {
        Boolean isValid = jwtUtil.validateToken(null, "test-user-id");
        
        assertFalse(isValid);
    }

    @Test
    void testExtractUserIdWithNullToken() {
        String extractedUserId = jwtUtil.extractUserId(null);
        
        assertNull(extractedUserId);
    }
}

