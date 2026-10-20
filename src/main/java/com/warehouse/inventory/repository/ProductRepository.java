package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
}
