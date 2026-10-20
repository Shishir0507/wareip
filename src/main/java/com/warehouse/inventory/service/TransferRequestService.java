package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.Role;
import com.warehouse.inventory.entity.Stock;
import com.warehouse.inventory.entity.TransferRequest;
import com.warehouse.inventory.entity.TransferStatus;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.BranchRepository;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockRepository;
import com.warehouse.inventory.repository.TransferRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransferRequestService {

    private final TransferRequestRepository transferRequestRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final PurchaseRequestService purchaseRequestService;

    public TransferRequestService(
            TransferRequestRepository transferRequestRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            PurchaseRequestService purchaseRequestService
    ) {
        this.transferRequestRepository = transferRequestRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.purchaseRequestService = purchaseRequestService;
    }

    public List<TransferRequest> getTransfersForUser(User user) {
        if (user.getRole() == Role.ADMIN) {
            return transferRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        if (user.getAssignedBranch() != null) {
            return transferRequestRepository.findByBranchInvolved(user.getAssignedBranch().getId());
        }

        return transferRequestRepository.findByRequestedByIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public TransferRequest createTransferRequest(User currentUser, Long destinationBranchId, Long productId, Integer quantity) {
        // 1. Validate that staff has an assigned branch
        Branch sourceBranch = currentUser.getAssignedBranch();
        if (sourceBranch == null) {
            throw new IllegalArgumentException("You do not have an assigned branch. Please contact an Administrator to assign your branch before creating transfer requests.");
        }

        // 2. Validate destination branch differs from source branch
        if (destinationBranchId == null) {
            throw new IllegalArgumentException("Destination branch is required");
        }
        if (sourceBranch.getId().equals(destinationBranchId)) {
            throw new IllegalArgumentException("Source branch and destination branch must be different.");
        }

        // 3. Validate product and quantity
        if (productId == null) {
            throw new IllegalArgumentException("Product is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be greater than 0");
        }

        Branch destinationBranch = branchRepository.findById(destinationBranchId)
                .orElseThrow(() -> new IllegalArgumentException("Destination branch not found with ID: " + destinationBranchId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // 4. Validate source stock is sufficient at creation time
        Stock sourceStock = stockRepository.findByBranchIdAndProductId(sourceBranch.getId(), productId)
                .orElseThrow(() -> new IllegalArgumentException("No stock record found for product '" + product.getName() + "' at source branch '" + sourceBranch.getName() + "'."));

        if (sourceStock.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock at source branch. Available: " + sourceStock.getQuantity() + ", Requested: " + quantity);
        }

        // 5. Create request with PENDING status
        TransferRequest request = new TransferRequest(
                sourceBranch,
                destinationBranch,
                product,
                quantity,
                currentUser,
                TransferStatus.PENDING
        );

        return transferRequestRepository.save(request);
    }

    @Transactional
    public TransferRequest approveTransfer(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer request not found with ID: " + id));

        if (request.getStatus() != TransferStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transfer requests can be approved. Current status: " + request.getStatus());
        }

        request.setStatus(TransferStatus.APPROVED);
        return transferRequestRepository.save(request);
    }

    @Transactional
    public TransferRequest rejectTransfer(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer request not found with ID: " + id));

        if (request.getStatus() == TransferStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reject a COMPLETED transfer request.");
        }
        if (request.getStatus() == TransferStatus.REJECTED) {
            throw new IllegalStateException("Transfer request is already rejected.");
        }

        request.setStatus(TransferStatus.REJECTED);
        return transferRequestRepository.save(request);
    }

    @Transactional
    public TransferRequest completeTransfer(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer request not found with ID: " + id));

        // Only an APPROVED request can be completed
        if (request.getStatus() != TransferStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED transfer requests can be completed. Current status: " + request.getStatus());
        }

        Long sourceBranchId = request.getSourceBranch().getId();
        Long destBranchId = request.getDestinationBranch().getId();
        Long productId = request.getProduct().getId();
        int transferQty = request.getQuantity();

        // 1. Re-verify source branch stock immediately before deduction
        Stock sourceStock = stockRepository.findByBranchIdAndProductId(sourceBranchId, productId)
                .orElseThrow(() -> new IllegalStateException("Source stock record not found for completion."));

        if (sourceStock.getQuantity() < transferQty) {
            throw new IllegalStateException("Cannot complete transfer: insufficient stock at source branch. Available: " + sourceStock.getQuantity() + ", Required: " + transferQty);
        }

        // 2. Deduct from source branch stock
        sourceStock.setQuantity(sourceStock.getQuantity() - transferQty);
        stockRepository.save(sourceStock);

        // 3. Add to destination branch stock (create record if not yet present)
        Stock destStock = stockRepository.findByBranchIdAndProductId(destBranchId, productId)
                .orElseGet(() -> new Stock(request.getDestinationBranch(), request.getProduct(), 0));

        destStock.setQuantity(destStock.getQuantity() + transferQty);
        stockRepository.save(destStock);

        // 4. Update request status to COMPLETED
        request.setStatus(TransferStatus.COMPLETED);
        TransferRequest completedRequest = transferRequestRepository.save(request);

        // 5. Check if source branch stock has fallen below threshold, triggering automatic purchase request
        purchaseRequestService.checkAndCreatePurchaseRequest(
                request.getSourceBranch(),
                request.getProduct(),
                sourceStock.getQuantity()
        );

        return completedRequest;
    }
}
