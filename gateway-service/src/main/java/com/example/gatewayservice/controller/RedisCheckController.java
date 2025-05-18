package com.example.gatewayservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
public class RedisCheckController {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @GetMapping("/redis-status")
    public Mono<ResponseEntity<Map<String, Object>>> checkRedisStatus() {
        return reactiveRedisTemplate.getConnectionFactory().getReactiveConnection()
                .ping()
                .map(pong -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "UP");
                    response.put("redis", "Connected");
                    response.put("ping", pong);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "DOWN");
                    response.put("redis", "Disconnected");
                    response.put("error", error.getMessage());
                    return Mono.just(ResponseEntity.ok(response));
                });
    }
}