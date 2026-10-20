package com.warehouse.inventory.service;

import com.warehouse.inventory.config.JwtUtil;
import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.entity.Role;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.BranchRepository;
import com.warehouse.inventory.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    public record AuthResult(
            String token,
            Long userId,
            String name,
            String email,
            String role,
            Long assignedBranchId,
            String assignedBranchName
    ) {}

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(
            UserRepository userRepository,
            BranchRepository branchRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public User registerStaff(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered: " + normalizedEmail);
        }

        // Public registration always enforces the STAFF role and no initial branch
        User staff = new User(
                name.trim(),
                normalizedEmail,
                passwordEncoder.encode(password.trim()),
                Role.STAFF,
                null
        );

        return userRepository.save(staff);
    }

    public AuthResult login(String email, String password) {
        if (email == null || password == null) {
            throw new BadCredentialsException("Email and password are required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password.trim(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getName());
        Long branchId = user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null;
        String branchName = user.getAssignedBranch() != null ? user.getAssignedBranch().getName() : null;

        return new AuthResult(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                branchId,
                branchName
        );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));
    }

    public List<User> getAllStaff() {
        return userRepository.findByRole(Role.STAFF);
    }

    @Transactional
    public User assignBranchToStaff(Long userId, Long branchId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found with ID: " + userId));

        if (user.getRole() != Role.STAFF) {
            throw new IllegalArgumentException("Branch assignment is only applicable to STAFF accounts");
        }

        if (branchId == null) {
            user.setAssignedBranch(null);
        } else {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found with ID: " + branchId));
            user.setAssignedBranch(branch);
        }

        return userRepository.save(user);
    }
}
