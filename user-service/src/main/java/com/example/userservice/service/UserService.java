package com.example.userservice.service;

import com.example.userservice.dto.UserResponse;
import com.example.userservice.exception.AppException;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return UserResponse.fromUser(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new AppException("Current user not found", HttpStatus.NOT_FOUND)));
    }

    @Transactional
    public UserResponse updateCurrentUser(String fullName) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Current user not found", HttpStatus.NOT_FOUND));

        user.setFullName(fullName);
        return UserResponse.fromUser(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(Long id) {
        return UserResponse.fromUser(
                userRepository.findById(id)
                        .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUser(Long id, String fullName, User.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setFullName(fullName);
        if (role != null) {
            user.setRole(role);
        }

        return UserResponse.fromUser(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new AppException("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
    }
}