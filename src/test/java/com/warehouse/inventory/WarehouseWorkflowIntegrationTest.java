package com.warehouse.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.inventory.config.JwtUtil;
import com.warehouse.inventory.controller.AuthController;
import com.warehouse.inventory.controller.BranchController;
import com.warehouse.inventory.controller.ProductController;
import com.warehouse.inventory.controller.StockController;
import com.warehouse.inventory.controller.TransferRequestController;
import com.warehouse.inventory.controller.UserController;
import com.warehouse.inventory.entity.Branch;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.PurchaseRequest;
import com.warehouse.inventory.entity.PurchaseStatus;
import com.warehouse.inventory.entity.Role;
import com.warehouse.inventory.entity.Stock;
import com.warehouse.inventory.entity.TransferRequest;
import com.warehouse.inventory.entity.TransferStatus;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.BranchRepository;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.PurchaseRequestRepository;
import com.warehouse.inventory.repository.StockRepository;
import com.warehouse.inventory.repository.TransferRequestRepository;
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.service.BranchService;
import com.warehouse.inventory.service.ProductService;
import com.warehouse.inventory.service.PurchaseRequestService;
import com.warehouse.inventory.service.StockService;
import com.warehouse.inventory.service.TransferRequestService;
import com.warehouse.inventory.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @Autowired
    private TransferRequestService transferRequestService;

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String staffToken;
    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.findByEmail("admin@warehouse.com").orElse(null);
        if (adminUser != null) {
            adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getName());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Admin bootstrap initialized default admin")
    void testAdminBootstrap() {
        assertNotNull(adminUser, "Bootstrap Admin user must exist");
        assertEquals(Role.ADMIN, adminUser.getRole());
    }

    @Test
    @Order(2)
    @DisplayName("Staff registration assigns STAFF role and unassigned branch")
    void testStaffRegistration() throws Exception {
        String uniqueEmail = "staff_" + System.currentTimeMillis() + "@warehouse.com";
        AuthController.RegisterRequest req = new AuthController.RegisterRequest("Staff Tester", uniqueEmail, "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"));

        User savedStaff = userRepository.findByEmail(uniqueEmail).orElseThrow();
        assertEquals(Role.STAFF, savedStaff.getRole());
        assertNull(savedStaff.getAssignedBranch(), "New staff should have no assigned branch initially");
    }

    @Test
    @Order(3)
    @DisplayName("Login returns valid JWT token")
    void testLogin() throws Exception {
        AuthController.LoginRequest loginReq = new AuthController.LoginRequest("admin@warehouse.com", "Admin@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @Order(4)
    @DisplayName("Unauthorized access is rejected")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("Staff cannot access Admin endpoints")
    void testStaffForbiddenOnAdminEndpoint() throws Exception {
        User staff = userService.registerStaff("Temporary Staff", "temp_" + System.currentTimeMillis() + "@test.com", "password123");
        String tempStaffToken = jwtUtil.generateToken(staff.getEmail(), staff.getRole().name(), staff.getName());

        Branch newBranch = new Branch("Unauthorized Branch", "Nowhere");

        mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + tempStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBranch)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("Transfer workflow, stock deduction, and automatic purchase request generation")
    void testCompleteTransferWorkflowAndAutoPurchase() throws Exception {
        // 1. Create two test branches
        Branch source = branchRepository.save(new Branch("Source Test Hub", "Building 1"));
        Branch dest = branchRepository.save(new Branch("Dest Test Hub", "Building 2"));

        // 2. Create a test product with threshold = 15
        Product product = productRepository.save(new Product("Test Gadget", "SKU-TST-" + System.currentTimeMillis(), "Description", 15));

        // 3. Initialize source stock with 20 items (above threshold 15)
        Stock sourceStock = stockRepository.save(new Stock(source, product, 20));

        // 4. Create and assign a staff member to the source branch
        String staffEmail = "assigned_staff_" + System.currentTimeMillis() + "@test.com";
        User staff = userService.registerStaff("Branch Staff", staffEmail, "password123");
        staff = userService.assignBranchToStaff(staff.getId(), source.getId());
        String testStaffToken = jwtUtil.generateToken(staff.getEmail(), staff.getRole().name(), staff.getName());

        // 5. Staff creates transfer request of 10 items (source will drop from 20 to 10, which is < 15 threshold!)
        TransferRequestController.TransferCreateRequest transferReq =
                new TransferRequestController.TransferCreateRequest(dest.getId(), product.getId(), 10);

        String responseStr = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + testStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        TransferRequest createdTransfer = objectMapper.readValue(responseStr, TransferRequest.class);
        Long transferId = createdTransfer.getId();

        // 6. Verify stock has NOT changed while PENDING
        Stock stockAfterPending = stockRepository.findByBranchIdAndProductId(source.getId(), product.getId()).orElseThrow();
        assertEquals(20, stockAfterPending.getQuantity(), "Stock must not be deducted while transfer is PENDING");

        // 7. Admin approves transfer
        mockMvc.perform(put("/api/transfers/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify stock has NOT changed while APPROVED
        Stock stockAfterApproved = stockRepository.findByBranchIdAndProductId(source.getId(), product.getId()).orElseThrow();
        assertEquals(20, stockAfterApproved.getQuantity(), "Stock must not be deducted on APPROVE");

        // 8. Admin completes transfer
        mockMvc.perform(put("/api/transfers/" + transferId + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 9. Verify stock deducted from source and added to destination
        Stock updatedSourceStock = stockRepository.findByBranchIdAndProductId(source.getId(), product.getId()).orElseThrow();
        assertEquals(10, updatedSourceStock.getQuantity(), "Source stock should decrease from 20 to 10");

        Stock updatedDestStock = stockRepository.findByBranchIdAndProductId(dest.getId(), product.getId()).orElseThrow();
        assertEquals(10, updatedDestStock.getQuantity(), "Destination stock should increase to 10");

        // 10. Verify automatic purchase request was generated because source stock (10) < threshold (15)
        boolean hasPendingPurchase = purchaseRequestRepository.existsByBranchIdAndProductIdAndStatus(
                source.getId(), product.getId(), PurchaseStatus.PENDING);
        assertTrue(hasPendingPurchase, "An automatic PENDING purchase request must be generated when stock drops below threshold");

        // Verify sensible requested quantity (15 - 10 = 5)
        List<PurchaseRequest> purchases = purchaseRequestRepository.findByBranchIdOrderByCreatedAtDesc(source.getId());
        assertFalse(purchases.isEmpty());
        PurchaseRequest pr = purchases.get(0);
        assertEquals(5, pr.getRequestedQuantity(), "Requested quantity should be difference to reach threshold (15 - 10 = 5)");

        // 11. Admin approves purchase request
        mockMvc.perform(put("/api/purchases/" + pr.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @Order(7)
    @DisplayName("Unsafe branch and product deletion is prevented")
    void testSafeDeletionEnforcement() {
        Branch branch = branchRepository.findAll().get(0);
        // Attempting to delete a branch that has stock or requests must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            branchService.deleteBranch(branch.getId());
        }, "Unsafe branch deletion should be blocked");

        Product product = productRepository.findAll().get(0);
        assertThrows(IllegalStateException.class, () -> {
            productService.deleteProduct(product.getId());
        }, "Unsafe product deletion should be blocked");
    }

    @Test
    @Order(8)
    @DisplayName("Invalid transfer requests are rejected")
    void testInvalidTransferRequests() {
        // Unassigned staff cannot transfer
        User unassigned = userService.registerStaff("Unassigned User", "unassigned_" + System.currentTimeMillis() + "@test.com", "pass123");
        Branch branch = branchRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);

        assertThrows(IllegalArgumentException.class, () -> {
            transferRequestService.createTransferRequest(unassigned, branch.getId(), product.getId(), 5);
        }, "Unassigned staff member should not be allowed to create transfer requests");
    }
}
