package com.warehouse.inventory.controller;

import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        User registered = userService.registerStaff(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Staff account registered successfully. An Administrator will assign your branch.",
                "userId", registered.getId(),
                "email", registered.getEmail(),
                "role", registered.getRole().name()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<UserService.AuthResult> login(@RequestBody LoginRequest request) {
        UserService.AuthResult authResult = userService.login(request.email(), request.password());
        return ResponseEntity.ok(authResult);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "assignedBranchId", user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : "",
                "assignedBranchName", user.getAssignedBranch() != null ? user.getAssignedBranch().getName() : "None Assigned"
        ));
    }
}
