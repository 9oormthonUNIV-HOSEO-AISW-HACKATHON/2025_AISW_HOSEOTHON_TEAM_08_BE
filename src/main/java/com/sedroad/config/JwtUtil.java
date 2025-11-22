package com.sedroad.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret이 설정되지 않았습니다.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret은 최소 32자 이상이어야 합니다.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String email) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("이메일이 필요합니다.");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        return createToken(claims, userId);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return extractClaim(token, claims -> claims.get("userId", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return extractClaim(token, claims -> claims.get("email", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    public Date extractExpiration(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("토큰이 필요합니다.");
        }
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("토큰 파싱 실패: " + e.getMessage(), e);
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            if (expiration == null) {
                return true;
            }
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token, String userId) {
        if (token == null || token.isEmpty() || userId == null || userId.isEmpty()) {
            return false;
        }
        try {
            final String extractedUserId = extractUserId(token);
            if (extractedUserId == null) {
                return false;
            }
            return (extractedUserId.equals(userId) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}

