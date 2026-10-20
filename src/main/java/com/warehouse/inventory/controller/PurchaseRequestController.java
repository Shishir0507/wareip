package com.warehouse.inventory.controller;

import com.warehouse.inventory.entity.PurchaseRequest;
import com.warehouse.inventory.service.PurchaseRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @GetMapping
    public ResponseEntity<List<PurchaseRequest>> getPurchaseRequests(
            @RequestParam(required = false) Long branchId
    ) {
        if (branchId != null) {
            return ResponseEntity.ok(purchaseRequestService.getPurchaseRequestsByBranch(branchId));
        }
        return ResponseEntity.ok(purchaseRequestService.getAllPurchaseRequests());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseRequest> approvePurchaseRequest(@PathVariable Long id) {
        PurchaseRequest approved = purchaseRequestService.approvePurchaseRequest(id);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseRequest> rejectPurchaseRequest(@PathVariable Long id) {
        PurchaseRequest rejected = purchaseRequestService.rejectPurchaseRequest(id);
        return ResponseEntity.ok(rejected);
    }
}
