package com.example.gatewayservice.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtGlobalFilter implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtGlobalFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    // Những đường dẫn nào không cần check token
    private static final String[] WHITELIST = {
            "/auth/login",
            "/auth/register",
            "/api/product"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        logger.info("Incoming request to path: {}", path);

        // 1. Bỏ qua những đường dẫn public
        for (String p : WHITELIST) {
            if (path.startsWith(p)) {
                logger.info("Path {} is whitelisted, skipping JWT validation.", path);
                return chain.filter(exchange);
            }
        }

        // 2. Lấy header Authorization
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        logger.info("Received token: {}", token);
        // logger.debug("Using JWT secret (Base64 encoded): {}", secret); // Log secret
        // gốc nếu cần

        try {
            // 3. Validate và parse token
            // Giải mã secret từ Base64 trước khi sử dụng
            byte[] decodedSecret = Base64.getDecoder().decode(secret);
            logger.debug("Successfully decoded JWT secret.");

            Claims claims = io.jsonwebtoken.Jwts.parser()
                    .setSigningKey(decodedSecret) // Sử dụng secret đã giải mã
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();

            @SuppressWarnings("unchecked")
            List<Object> rolesClaimRaw = claims.get("roles", List.class);
            String roles = "";
            if (rolesClaimRaw != null && !rolesClaimRaw.isEmpty()) {
                List<String> actualRoles = new ArrayList<>();
                for (Object roleObj : rolesClaimRaw) {
                    if (roleObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> roleMap = (Map<String, String>) roleObj;
                        if (roleMap.containsKey("authority")) {
                            actualRoles.add(roleMap.get("authority"));
                        }
                    }
                }
                if (!actualRoles.isEmpty()) {
                    roles = String.join(",", actualRoles);
                } else {
                    logger.warn("No 'authority' found in roles claim for user: {}", username);
                }
            } else {
                logger.warn("Roles claim is missing or empty in JWT for user: {}", username);
            }

            logger.info("Token validated successfully for user: {}, roles: [{}]", username, roles);

            // 4. Thêm thông tin user vào header request
            ServerHttpRequest.Builder mutatedRequestBuilder = exchange.getRequest().mutate()
                    .header("X-USER", username);

            if (!roles.isEmpty()) {
                mutatedRequestBuilder.header("X-ROLES", roles);
            }

            ServerHttpRequest mutatedReq = mutatedRequestBuilder.build();

            return chain.filter(exchange.mutate().request(mutatedReq).build());

        } catch (IllegalArgumentException e) {
            logger.error("Error decoding Base64 secret: {}", e.getMessage());
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing JWT secret");
        } catch (JwtException e) {
            logger.error("JWT token validation error for token: {}. Error: {}", token, e.getMessage());
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "JWT token invalid or expired");
        }
    }
}
