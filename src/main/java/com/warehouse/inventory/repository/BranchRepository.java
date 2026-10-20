package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByName(String name);
}
