package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/auth/**", "/users/**", "/me/**")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter))
                                .rewritePath("/(?<segment>.*)", "/${segment}"))
                        .uri("lb://user-service"))

                // Inventory Service
                .route("inventory-service", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter))
                                .rewritePath("/(?<segment>.*)", "/${segment}"))
                        .uri("lb://inventory-service"))

                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**", "/api/cart/**")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter))
                                .rewritePath("/(?<segment>.*)", "/${segment}"))
                        .uri("lb://order-service"))

                // Product Service
                .route("product-service", r -> r
                        .path("/api/product/**", "/api/manufacture/**", "/api/category/**")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter))
                                .rewritePath("/(?<segment>.*)", "/${segment}"))
                        .uri("lb://product-service"))

                // Payment Service
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter))
                                .rewritePath("/(?<segment>.*)", "/${segment}"))
                        .uri("lb://payment-service"))

                // AI Chat Service
                .route("ai-chat-service", r -> r
                        .path("/api/chat/**")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter))
                                .rewritePath("/(?<segment>.*)", "/${segment}"))
                        .uri("lb://ai-chat-service"))
                .build();
    }
}