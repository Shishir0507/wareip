package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.repository.BranchRepository;
import com.warehouse.inventory.repository.PurchaseRequestRepository;
import com.warehouse.inventory.repository.StockRepository;
import com.warehouse.inventory.repository.TransferRequestRepository;
import com.warehouse.inventory.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    public BranchService(
            BranchRepository branchRepository,
            StockRepository stockRepository,
            UserRepository userRepository,
            TransferRequestRepository transferRequestRepository,
            PurchaseRequestRepository purchaseRequestRepository
    ) {
        this.branchRepository = branchRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found with ID: " + id));
    }

    @Transactional
    public Branch createBranch(Branch branch) {
        if (branch.getName() == null || branch.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name is required");
        }
        if (branch.getLocation() == null || branch.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Branch location is required");
        }
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long id, Branch updated) {
        Branch branch = getBranchById(id);
        if (updated.getName() == null || updated.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name is required");
        }
        if (updated.getLocation() == null || updated.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Branch location is required");
        }
        branch.setName(updated.getName().trim());
        branch.setLocation(updated.getLocation().trim());
        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = getBranchById(id);

        if (stockRepository.existsByBranchId(id)) {
            throw new IllegalStateException("Cannot delete branch '" + branch.getName() + "': inventory stock records exist for this branch.");
        }
        if (userRepository.existsByAssignedBranchId(id)) {
            throw new IllegalStateException("Cannot delete branch '" + branch.getName() + "': staff members are currently assigned to this branch.");
        }
        if (transferRequestRepository.existsBySourceBranchIdOrDestinationBranchId(id, id)) {
            throw new IllegalStateException("Cannot delete branch '" + branch.getName() + "': historical transfer requests reference this branch.");
        }
        if (purchaseRequestRepository.existsByBranchId(id)) {
            throw new IllegalStateException("Cannot delete branch '" + branch.getName() + "': purchase requests reference this branch.");
        }

        branchRepository.delete(branch);
    }
}
