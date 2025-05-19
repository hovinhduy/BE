package com.iuh.fit.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtExtractor {

    private final JwtService jwtService;

    public Long extractUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        return jwtService.extractUserId(token);
    }

    public String extractUsername(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        return jwtService.extractUsername(token);
    }

    public boolean hasRole(HttpServletRequest request, String role) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return false;
        }
        
        return jwtService.extractRoles(token).stream()
                .anyMatch(r -> r.get("authority").equals(role));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
} 