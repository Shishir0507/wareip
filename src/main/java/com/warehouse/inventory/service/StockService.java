package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.Stock;
import com.warehouse.inventory.repository.BranchRepository;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    public record StockView(
            Long id,
            Long branchId,
            String branchName,
            Long productId,
            String productName,
            String productSku,
            Integer quantity,
            Integer minimumStockThreshold,
            String status
    ) {}

    private final StockRepository stockRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final PurchaseRequestService purchaseRequestService;

    public StockService(
            StockRepository stockRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            PurchaseRequestService purchaseRequestService
    ) {
        this.stockRepository = stockRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.purchaseRequestService = purchaseRequestService;
    }

    public List<StockView> getStocks(Long branchId) {
        List<Stock> stocks;
        if (branchId != null) {
            stocks = stockRepository.findByBranchId(branchId);
        } else {
            stocks = stockRepository.findAll();
        }

        return stocks.stream().map(this::toStockView).collect(Collectors.toList());
    }

    @Transactional
    public Stock initializeStock(Long branchId, Long productId, Integer quantity) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be greater than or equal to 0");
        }

        if (stockRepository.existsByBranchIdAndProductId(branchId, productId)) {
            throw new IllegalArgumentException("A stock record already exists for this branch and product. Duplicate records are not allowed.");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found with ID: " + branchId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        Stock stock = new Stock(branch, product, quantity);
        Stock savedStock = stockRepository.save(stock);

        // Check if initial stock quantity falls below the threshold
        purchaseRequestService.checkAndCreatePurchaseRequest(branch, product, quantity);

        return savedStock;
    }

    private StockView toStockView(Stock stock) {
        int threshold = stock.getProduct().getMinimumStockThreshold();
        String status = stock.getQuantity() < threshold ? "LOW_STOCK" : "IN_STOCK";

        return new StockView(
                stock.getId(),
                stock.getBranch().getId(),
                stock.getBranch().getName(),
                stock.getProduct().getId(),
                stock.getProduct().getName(),
                stock.getProduct().getSku(),
                stock.getQuantity(),
                threshold,
                status
        );
    }
}
