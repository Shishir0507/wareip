package com.warehouse.inventory.controller;

import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public record StaffView(
            Long id,
            String name,
            String email,
            Long branchId,
            String branchName
    ) {}

    public record AssignBranchRequest(Long branchId) {}

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffView>> getAllStaff() {
        List<User> staffList = userService.getAllStaff();
        List<StaffView> views = staffList.stream().map(u -> new StaffView(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getAssignedBranch() != null ? u.getAssignedBranch().getId() : null,
                u.getAssignedBranch() != null ? u.getAssignedBranch().getName() : "Unassigned"
        )).collect(Collectors.toList());

        return ResponseEntity.ok(views);
    }

    @PutMapping("/staff/{userId}/branch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffView> assignBranch(
            @PathVariable Long userId,
            @RequestBody AssignBranchRequest request
    ) {
        User updated = userService.assignBranchToStaff(userId, request.branchId());
        StaffView view = new StaffView(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getAssignedBranch() != null ? updated.getAssignedBranch().getId() : null,
                updated.getAssignedBranch() != null ? updated.getAssignedBranch().getName() : "Unassigned"
        );
        return ResponseEntity.ok(view);
    }
}
