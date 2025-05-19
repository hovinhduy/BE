package com.iuh.fit.order_service.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtValidator {

    private final JwtService jwtService;

    /**
     * Kiểm tra tính hợp lệ của JWT
     * @param token JWT cần kiểm tra
     * @return true nếu token hợp lệ, ngược lại là false
     */
    public boolean validateToken(String token) {
        try {
            // Thử trích xuất thông tin từ token để kiểm tra tính hợp lệ
            jwtService.extractAllClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Chữ ký JWT không hợp lệ: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT không đúng định dạng: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT đã hết hạn: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT trống: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không xác định khi xác thực JWT: {}", e.getMessage());
        }
        return false;
    }
} 