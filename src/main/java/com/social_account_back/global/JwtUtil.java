package com.social_account_back.global;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final StringRedisTemplate redisTemplate;

    public JwtUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Value("${jwt.secret}")
    private String secretKey;

    private long accessTokenExpireMs = 60 * 60 * 1000L; // 1시간
    private long refreshTokenExpireMs = 30L * 24 * 60 * 60 * 1000; // 30일

    public String generateAccessToken(Long kakaoId, String nickname, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(kakaoId))
                .claim("nickname", nickname)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpireMs))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long kakaoId) {
        String refreshToken = UUID.randomUUID().toString();

        String redisKey = "refresh:" + kakaoId;

        redisTemplate.opsForValue().set(redisKey, refreshToken, Duration.ofMillis(refreshTokenExpireMs));

        return refreshToken;
    }

    public boolean isRefreshTokenValid(Long kakaoId, String refreshToken) {
        String key = "refresh:" + kakaoId;
        String storedToken = redisTemplate.opsForValue().get(key);
        return refreshToken.equals(storedToken);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Claims parseToken(String token) throws ExpiredJwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public void deleteRefreshToken(Long kakaoId) {
        String key = "refresh:" + kakaoId;
        redisTemplate.delete(key);
    }

    public long getAccessTokenExpireMs() {
        return accessTokenExpireMs;
    }
}
