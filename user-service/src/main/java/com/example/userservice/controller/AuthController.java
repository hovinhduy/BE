package com.example.userservice.controller;

import com.example.userservice.dto.ApiResponse;
import com.example.userservice.dto.AuthRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserRegistrationResponse;
import com.example.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserRegistrationResponse userResponse = authService.register(request);
        ApiResponse<UserRegistrationResponse> response = ApiResponse.success(userResponse,
                "User register successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody AuthRequest request) {
        String token = authService.login(request);
        Map<String, String> tokenResponse = new HashMap<>();
        tokenResponse.put("token", token);

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                tokenResponse, "Login successful");
        return ResponseEntity.ok(response);
    }
}