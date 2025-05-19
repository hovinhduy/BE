package com.iuh.fit.order_service.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

@Component
@RequestScope
@Data
@NoArgsConstructor
public class UserContext {
    private Long userId;
    private String username;
    private List<String> roles = new ArrayList<>();
    
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
} 