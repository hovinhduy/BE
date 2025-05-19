package com.iuh.fit.order_service.security;

import com.iuh.fit.order_service.exception.ForbiddenException;
import com.iuh.fit.order_service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserContext userContext;

    /**
     * Đảm bảo người dùng đã đăng nhập, nếu không ném ngoại lệ UnauthorizedException
     * @return ID của người dùng hiện tại đã đăng nhập
     */
    public Long getCurrentUserId() {
        Long userId = userContext.getUserId();
        if (userId == null) {
            throw new UnauthorizedException("Bạn cần đăng nhập để thực hiện thao tác này");
        }
        return userId;
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò đã cho không
     * @param role Vai trò cần kiểm tra
     * @return true nếu người dùng có vai trò, ngược lại là false
     */
    public boolean hasRole(String role) {
        return userContext.hasRole(role);
    }

    /**
     * Đảm bảo người dùng có vai trò admin, nếu không ném ngoại lệ ForbiddenException
     */
    public void requireAdmin() {
        getCurrentUserId(); // Đảm bảo đã đăng nhập
        if (!hasRole("ROLE_ADMIN")) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    /**
     * Đảm bảo người dùng có ID chỉ định hoặc là admin
     * @param resourceUserId ID của người dùng sở hữu tài nguyên
     */
    public void requireOwnerOrAdmin(Long resourceUserId) {
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(resourceUserId) && !hasRole("ROLE_ADMIN")) {
            throw new ForbiddenException("Bạn không có quyền truy cập tài nguyên này");
        }
    }
} 