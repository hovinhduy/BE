package com.example.userservice.service;

import com.example.userservice.dto.AuthRequest;
import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.exception.AppException;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.jwt.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(@Valid RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(User.Role.USER);

        User savedUser = userRepository.save(user);
        String jwt = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(jwt)
                .user(UserResponse.fromUser(savedUser))
                .build();
    }

    public AuthResponse login(@Valid AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwt = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwt)
                .user(UserResponse.fromUser(user))
                .build();
    }
}