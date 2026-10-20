package com.warehouse.inventory.config;

import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.Role;
import com.warehouse.inventory.entity.Stock;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.BranchRepository;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockRepository;
import com.warehouse.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@warehouse.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    public AdminDataInitializer(
            UserRepository userRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Safe Admin Bootstrap
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User(
                    adminName,
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN,
                    null
            );
            userRepository.save(admin);
            System.out.println(">>> Initialized default Admin user: " + adminEmail);
        }

        // 2. Minimal Sample Demonstration Data (Only if database is completely empty)
        if (branchRepository.count() == 0 && productRepository.count() == 0) {
            Branch b1 = branchRepository.save(new Branch("Central Warehouse", "Building A, Logistics Park"));
            Branch b2 = branchRepository.save(new Branch("East Distribution Center", "Sector 5, Industrial Area"));

            Product p1 = productRepository.save(new Product("Wireless Optical Mouse", "PRD-MOU-001", "Ergonomic 2.4GHz wireless mouse", 10));
            Product p2 = productRepository.save(new Product("Mechanical Gaming Keyboard", "PRD-KEY-002", "RGB mechanical blue switches", 8));
            Product p3 = productRepository.save(new Product("27-inch 4K Monitor", "PRD-MON-003", "IPS UltraHD 60Hz display", 5));

            // Seed initial stock
            stockRepository.save(new Stock(b1, p1, 25));
            stockRepository.save(new Stock(b1, p2, 15));
            stockRepository.save(new Stock(b1, p3, 10));

            stockRepository.save(new Stock(b2, p1, 12));
            stockRepository.save(new Stock(b2, p2, 5)); // note: 5 is below threshold 8, low stock demo
            stockRepository.save(new Stock(b2, p3, 6));

            System.out.println(">>> Minimal demonstration branches, products, and stock initialized.");
        }
    }
}
