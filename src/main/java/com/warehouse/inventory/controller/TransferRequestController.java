package com.warehouse.inventory.controller;

import com.warehouse.inventory.entity.TransferRequest;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.service.TransferRequestService;
import com.warehouse.inventory.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferRequestController {

    public record TransferCreateRequest(Long destinationBranchId, Long productId, Integer quantity) {}

    private final TransferRequestService transferRequestService;
    private final UserService userService;

    public TransferRequestController(TransferRequestService transferRequestService, UserService userService) {
        this.transferRequestService = transferRequestService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<TransferRequest>> getTransfers(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(transferRequestService.getTransfersForUser(currentUser));
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<TransferRequest> createTransfer(
            @RequestBody TransferCreateRequest request,
            Authentication authentication
    ) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        TransferRequest created = transferRequestService.createTransferRequest(
                currentUser,
                request.destinationBranchId(),
                request.productId(),
                request.quantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransferRequest> approveTransfer(@PathVariable Long id) {
        TransferRequest approved = transferRequestService.approveTransfer(id);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransferRequest> rejectTransfer(@PathVariable Long id) {
        TransferRequest rejected = transferRequestService.rejectTransfer(id);
        return ResponseEntity.ok(rejected);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransferRequest> completeTransfer(@PathVariable Long id) {
        TransferRequest completed = transferRequestService.completeTransfer(id);
        return ResponseEntity.ok(completed);
    }
}
