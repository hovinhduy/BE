package com.iuh.fit.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final JwtValidator jwtValidator;
    private final UserContext userContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null && jwtValidator.validateToken(token)) {
                Long userId = jwtService.extractUserId(token);
                String username = jwtService.extractUsername(token);
                List<Map<String, String>> roles = jwtService.extractRoles(token);
                
                userContext.setUserId(userId);
                userContext.setUsername(username);
                
                // Trích xuất các role từ JWT
                List<String> userRoles = roles.stream()
                        .map(role -> role.get("authority"))
                        .toList();
                userContext.setRoles(userRoles);
                
                log.debug("Đã xác thực người dùng: {}, userId: {}, roles: {}", username, userId, userRoles);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xác thực JWT: {}", e.getMessage());
            // Không trả về lỗi, chỉ ghi log và tiếp tục xử lý request
        }
        
        // Luôn cho phép request tiếp tục, việc kiểm tra quyền sẽ được thực hiện ở các controller
        return true;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
} 