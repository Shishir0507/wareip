package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    
    @Query("SELECT t FROM TransferRequest t WHERE t.sourceBranch.id = :branchId OR t.destinationBranch.id = :branchId ORDER BY t.createdAt DESC")
    List<TransferRequest> findByBranchInvolved(@Param("branchId") Long branchId);

    List<TransferRequest> findByRequestedByIdOrderByCreatedAtDesc(Long requestedById);

    List<TransferRequest> findAllByOrderByCreatedAtDesc();

    boolean existsBySourceBranchIdOrDestinationBranchId(Long sourceBranchId, Long destinationBranchId);

    boolean existsByProductId(Long productId);
}
