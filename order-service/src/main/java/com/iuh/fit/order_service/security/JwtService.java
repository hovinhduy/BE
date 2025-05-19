package com.iuh.fit.order_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims.get("userId") != null) {
                return ((Number) claims.get("userId")).longValue();
            }
            return null;
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất userId từ token: {}", e.getMessage());
            return null;
        }
    }

    public List<Map<String, String>> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("roles", List.class);
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất roles từ token: {}", e.getMessage());
            return List.of();
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 