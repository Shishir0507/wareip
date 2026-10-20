package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.PurchaseRequest;
import com.warehouse.inventory.entity.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    boolean existsByBranchIdAndProductIdAndStatus(Long branchId, Long productId, PurchaseStatus status);

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    List<PurchaseRequest> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    boolean existsByBranchId(Long branchId);

    boolean existsByProductId(Long productId);
}
