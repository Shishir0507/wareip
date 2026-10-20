package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.PurchaseRequestRepository;
import com.warehouse.inventory.repository.StockRepository;
import com.warehouse.inventory.repository.TransferRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    public ProductService(
            ProductRepository productRepository,
            StockRepository stockRepository,
            TransferRequestRepository transferRequestRepository,
            PurchaseRequestRepository purchaseRequestRepository
    ) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            throw new IllegalArgumentException("Product SKU is required");
        }
        if (product.getMinimumStockThreshold() == null || product.getMinimumStockThreshold() < 0) {
            throw new IllegalArgumentException("Minimum stock threshold must be a non-negative number");
        }

        String normalizedSku = product.getSku().trim().toUpperCase();
        if (productRepository.existsBySku(normalizedSku)) {
            throw new IllegalArgumentException("Product with SKU '" + normalizedSku + "' already exists");
        }

        product.setName(product.getName().trim());
        product.setSku(normalizedSku);
        if (product.getDescription() != null) {
            product.setDescription(product.getDescription().trim());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product updated) {
        Product product = getProductById(id);

        if (updated.getName() == null || updated.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (updated.getSku() == null || updated.getSku().trim().isEmpty()) {
            throw new IllegalArgumentException("Product SKU is required");
        }
        if (updated.getMinimumStockThreshold() == null || updated.getMinimumStockThreshold() < 0) {
            throw new IllegalArgumentException("Minimum stock threshold must be a non-negative number");
        }

        String normalizedSku = updated.getSku().trim().toUpperCase();
        if (productRepository.existsBySkuAndIdNot(normalizedSku, id)) {
            throw new IllegalArgumentException("Another product already has SKU: " + normalizedSku);
        }

        product.setName(updated.getName().trim());
        product.setSku(normalizedSku);
        product.setDescription(updated.getDescription() != null ? updated.getDescription().trim() : null);
        product.setMinimumStockThreshold(updated.getMinimumStockThreshold());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);

        if (stockRepository.existsByProductId(id)) {
            throw new IllegalStateException("Cannot delete product '" + product.getName() + "': stock records exist for this product.");
        }
        if (transferRequestRepository.existsByProductId(id)) {
            throw new IllegalStateException("Cannot delete product '" + product.getName() + "': transfer requests reference this product.");
        }
        if (purchaseRequestRepository.existsByProductId(id)) {
            throw new IllegalStateException("Cannot delete product '" + product.getName() + "': purchase requests reference this product.");
        }

        productRepository.delete(product);
    }
}
