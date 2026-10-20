package com.warehouse.inventory.controller;

import com.warehouse.inventory.entity.Stock;
import com.warehouse.inventory.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    public record StockInitRequest(Long branchId, Long productId, Integer quantity) {}

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<StockService.StockView>> getStocks(
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(stockService.getStocks(branchId));
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Stock> initializeStock(@RequestBody StockInitRequest request) {
        Stock initialized = stockService.initializeStock(
                request.branchId(),
                request.productId(),
                request.quantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(initialized);
    }
}
