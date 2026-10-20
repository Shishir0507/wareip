package com.warehouse.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer minimumStockThreshold;

    public Product() {
    }

    public Product(String name, String sku, String description, Integer minimumStockThreshold) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.minimumStockThreshold = minimumStockThreshold;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMinimumStockThreshold() {
        return minimumStockThreshold;
    }

    public void setMinimumStockThreshold(Integer minimumStockThreshold) {
        this.minimumStockThreshold = minimumStockThreshold;
    }
}
