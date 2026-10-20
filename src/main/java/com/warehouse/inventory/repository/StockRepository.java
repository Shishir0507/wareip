package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByBranchIdAndProductId(Long branchId, Long productId);
    List<Stock> findByBranchId(Long branchId);
    boolean existsByBranchId(Long branchId);
    boolean existsByProductId(Long productId);
    boolean existsByBranchIdAndProductId(Long branchId, Long productId);
}
