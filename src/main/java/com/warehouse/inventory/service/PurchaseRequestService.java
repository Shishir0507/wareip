package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.PurchaseRequest;
import com.warehouse.inventory.entity.PurchaseStatus;
import com.warehouse.inventory.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;

    public PurchaseRequestService(PurchaseRequestRepository purchaseRequestRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    public List<PurchaseRequest> getAllPurchaseRequests() {
        return purchaseRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PurchaseRequest> getPurchaseRequestsByBranch(Long branchId) {
        return purchaseRequestRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
    }

    @Transactional
    public void checkAndCreatePurchaseRequest(Branch branch, Product product, int currentQuantity) {
        int threshold = product.getMinimumStockThreshold();

        // Check if stock is below the configured threshold
        if (currentQuantity < threshold) {
            // Check if there is already a PENDING purchase request for this branch and product
            boolean pendingExists = purchaseRequestRepository.existsByBranchIdAndProductIdAndStatus(
                    branch.getId(),
                    product.getId(),
                    PurchaseStatus.PENDING
            );

            if (!pendingExists) {
                // Requested quantity brings stock back up to the threshold
                int requestedQuantity = threshold - currentQuantity;
                if (requestedQuantity <= 0) {
                    requestedQuantity = threshold;
                }

                PurchaseRequest request = new PurchaseRequest(
                        branch,
                        product,
                        requestedQuantity,
                        PurchaseStatus.PENDING
                );
                purchaseRequestRepository.save(request);
            }
        }
    }

    @Transactional
    public PurchaseRequest approvePurchaseRequest(Long id) {
        PurchaseRequest request = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase request not found with ID: " + id));

        if (request.getStatus() != PurchaseStatus.PENDING) {
            throw new IllegalStateException("Only PENDING purchase requests can be approved. Current status: " + request.getStatus());
        }

        request.setStatus(PurchaseStatus.APPROVED);
        return purchaseRequestRepository.save(request);
    }

    @Transactional
    public PurchaseRequest rejectPurchaseRequest(Long id) {
        PurchaseRequest request = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase request not found with ID: " + id));

        if (request.getStatus() != PurchaseStatus.PENDING) {
            throw new IllegalStateException("Only PENDING purchase requests can be rejected. Current status: " + request.getStatus());
        }

        request.setStatus(PurchaseStatus.REJECTED);
        return purchaseRequestRepository.save(request);
    }
}
