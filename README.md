# Multi-Branch Warehouse Inventory Management System

A clean, maintainable, student-friendly Java Spring Boot web application for multi-branch warehouse inventory tracking, inter-branch stock transfers, and automated low-stock purchase requests.

---

## 1. Technology Stack

- **Java**: Version 21 (runs seamlessly on Java 21–24)
- **Framework**: Spring Boot 3.3.5 (Maven)
- **Database**: MySQL 8.0+
- **Persistence**: Spring Data JPA & Hibernate
- **Security**: Spring Security 6 with stateless JWT (JSON Web Token) authentication & BCrypt password hashing
- **Frontend**: Clean Vanilla JavaScript (`fetch` API), Semantic HTML5, and CSS3 (Zero external frameworks/libraries)
- **IDE**: IntelliJ IDEA (or any standard Java IDE)

---

## 2. Project Architecture & Folder Structure

The project adheres strictly to standard, clean Spring Boot packaging without unnecessary layers or duplicate DTOs:

```
c:\warehouseip\
│   pom.xml
│   init.sql
│   README.md
│
├───src
│   ├───main
│   │   ├───java
│   │   │   └───com
│   │   │       └───warehouse
│   │   │           └───inventory
│   │   │               │   WarehouseInventoryApplication.java
│   │   │               │
│   │   │               ├───config
│   │   │               │       AdminDataInitializer.java    # Seeds initial Admin and sample catalog
│   │   │               │       GlobalExceptionHandler.java  # Formats JSON error responses
│   │   │               │       JwtAuthenticationFilter.java # Intercepts requests & validates Bearer token
│   │   │               │       JwtUtil.java                 # Generates & parses HMAC-SHA256 tokens
│   │   │               │       SecurityConfig.java          # Configures URL permissions & password encoder
│   │   │               │
│   │   │               ├───controller
│   │   │               │       AuthController.java          # /api/auth (register, login, me)
│   │   │               │       BranchController.java        # /api/branches (CRUD)
│   │   │               │       ProductController.java       # /api/products (CRUD)
│   │   │               │       PurchaseRequestController.java # /api/purchases (view, approve, reject)
│   │   │               │       StockController.java         # /api/stocks (overview, initialize)
│   │   │               │       TransferRequestController.java# /api/transfers (create, approve, complete)
│   │   │               │       UserController.java          # /api/users (staff list, assign branch)
│   │   │               │
│   │   │               ├───entity
│   │   │               │       Branch.java                  # Warehouse location (id, name, location)
│   │   │               │       Product.java                 # Item catalog (id, name, sku, threshold)
│   │   │               │       PurchaseStatus.java          # Enum (PENDING, APPROVED, REJECTED)
│   │   │               │       PurchaseRequest.java         # Automated restock order entity
│   │   │               │       Role.java                    # Enum (ADMIN, STAFF)
│   │   │               │       Stock.java                   # Branch inventory balance (enforces unique branch/product)
│   │   │               │       TransferRequest.java         # Inter-branch transfer workflow entity
│   │   │               │       TransferStatus.java          # Enum (PENDING, APPROVED, REJECTED, COMPLETED)
│   │   │               │       User.java                    # System user (role, hashed password, assignedBranch)
│   │   │               │
│   │   │               ├───repository
│   │   │               │       BranchRepository.java
│   │   │               │       ProductRepository.java
│   │   │               │       PurchaseRequestRepository.java
│   │   │               │       StockRepository.java
│   │   │               │       TransferRequestRepository.java
│   │   │               │       UserRepository.java
│   │   │               │
│   │   │               └───service
│   │   │                       BranchService.java
│   │   │                       ProductService.java
│   │   │                       PurchaseRequestService.java
│   │   │                       StockService.java
│   │   │                       TransferRequestService.java
│   │   │                       UserService.java
│   │   │
│   │   └───resources
│   │       │   application.properties
│   │       └───static
│   │               index.html                               # Login and Staff Registration
│   │               dashboard.html                           # Real-time KPIs & Multi-Branch Stock Overview
│   │               management.html                          # Branches, Products & Staff Assignment (Admin)
│   │               transfers.html                           # Inter-Branch Stock Transfers & Approvals
│   │               purchases.html                           # Automated Restock / Purchase Requests
│   │               script.js                                # Shared vanilla JavaScript API client
│   │               style.css                                # Shared responsive CSS styling
│   │
│   └───test
│       └───java
│           └───com
│               └───warehouse
│                   └───inventory
│                           WarehouseWorkflowIntegrationTest.java # Comprehensive JUnit 5 integration test suite
```

---

## 3. Database Setup (MySQL)

1. Ensure MySQL Server is running (e.g. MySQL 8.0 on default port `3306`).
2. Log into MySQL and create the database:
   ```sql
   CREATE DATABASE warehouse_inventory;
   ```
   *(Or run the provided `init.sql` script)*.
3. Open `src/main/resources/application.properties` and verify your credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:warehouse_inventory}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=${DB_USERNAME:root}
   spring.datasource.password=${DB_PASSWORD:102007}
   ```
   > You can override any property using environment variables (`DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `DB_HOST`).

Hibernate will automatically create and update the required tables (`ddl-auto=update`).

---

## 4. Initial Admin Bootstrap & Credentials

To ensure security while keeping setup friction-free for demonstration, the application features an automatic, idempotent bootstrap runner (`AdminDataInitializer`):

- When the application starts, it checks if an Administrator account exists.
- If none exists, it securely creates the default Admin using BCrypt password hashing.
- It also initializes minimal demonstration branches, products, and initial stocks only if the tables are completely empty. It **never** duplicates data on subsequent restarts.

### Pre-Configured Credentials:
- **Admin Email**: `admin@warehouse.com`
- **Admin Password**: `Admin@123`
- **Role**: `ADMIN`

*(Credentials can be customized in `application.properties` or overridden via `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables).*

---

## 5. How to Run in IntelliJ IDEA

1. Launch **IntelliJ IDEA**.
2. Click **Open** and select the folder `c:\warehouseip`.
3. IntelliJ will detect the `pom.xml` and import the Maven dependencies automatically.
4. Ensure your Project SDK is set to **Java 21** or higher:
   - Navigate to `File` → `Project Structure` → `Project` → `SDK` (select Java 21 or installed JDK).
5. Locate the main class:
   - Path: `src/main/java/com/warehouse/inventory/WarehouseInventoryApplication.java`
6. Right-click `WarehouseInventoryApplication` and select **Run 'WarehouseInventoryApplication'**.
7. The embedded Tomcat server will start on port `8080`.

---

## 6. How to Run via Maven Command Line

From the project root:

```powershell
# Compile and package the executable jar
mvn clean package -DskipTests

# Run the Spring Boot application
java -jar target/inventory-management-1.0.0.jar
```

Or run directly via the Spring Boot Maven plugin:
```powershell
mvn spring-boot:run
```

To run all automated integration tests:
```powershell
mvn test
```

---

## 7. Accessing the Application & Five-Page Architecture

Open your browser and navigate to:
**`http://localhost:8080`**

The user interface is organized into five dedicated pages:
1. **`index.html`** *(Authentication)*: Public login and Staff account registration forms. Authenticated users are automatically redirected to `dashboard.html`.
2. **`dashboard.html`** *(Dashboard Analytics & Stock)*: Displays live system KPIs (Total Branches, Catalog Products, Low-Stock items, Pending Transfers, and Pending Purchases computed from real API data) and the branch-filtered stock inventory table. Administrators can also initialize new branch stock here.
3. **`management.html`** *(Administrative Management - Admin Only)*: Full CRUD management for physical branches and product catalog, as well as staff warehouse branch assignments.
4. **`transfers.html`** *(Inter-Branch Transfers)*: Staff create transfer requests from their assigned branch; Administrators review, approve, reject, and complete stock deductions.
5. **`purchases.html`** *(Automated Purchase Requests)*: Lists system-generated restock orders with Admin approval and rejection controls.

### User Roles & Permissions Matrix

| Feature / Action | Admin | Staff |
|---|:---:|:---:|
| Register new account | N/A (Admin bootstrapped) | ✅ (Always assigned STAFF role) |
| Log in with JWT | ✅ | ✅ |
| View Multi-Branch Stock Overview | ✅ (All branches) | ✅ (All branches) |
| Filter Stock by Branch | ✅ | ✅ |
| Low Stock Indicator (`< threshold`) | ✅ | ✅ |
| Create Transfer Request | ❌ (Transfers are made by branch staff) | ✅ (From assigned branch only) |
| View Transfer Requests | ✅ (All requests) | ✅ (Requests involving staff's branch) |
| Approve / Reject Transfer Requests | ✅ | ❌ |
| Complete Approved Transfer (Deduct & Add Stock) | ✅ | ❌ |
| Automatic Purchase Request Generation | ✅ (Triggered by system) | ✅ (Triggered by system) |
| View Purchase Requests | ✅ | ✅ |
| Approve / Reject Purchase Requests | ✅ | ❌ |
| Add / Edit / Delete Branches (Safe Deletion) | ✅ | ❌ |
| Add / Edit / Delete Products (Safe Deletion) | ✅ | ❌ |
| Assign Branch to Staff Members | ✅ | ❌ |
| Initialize Branch Stock (One per Branch/Product) | ✅ | ❌ |

---

## 8. Key Business Workflows Explained

### Inter-Branch Stock Transfer Workflow
1. **Creation**: A logged-in Staff member initiates a transfer request. The source branch is **automatically enforced** from the staff member's assigned branch to prevent impersonation.
2. **Pre-Check**: Validates that destination != source, quantity > 0, and source stock has sufficient quantity. Status is saved as `PENDING`.
3. **Approval**: An Admin reviews the transfer and can click **Approve** (sets status to `APPROVED`) or **Reject** (sets status to `REJECTED`).
   - *Important*: Stock is **not** deducted upon approval.
4. **Completion**: Only an `APPROVED` transfer can be completed by an Admin.
   - Upon clicking **Complete Transfer**, the backend executes an atomic `@Transactional` operation:
     - Re-verifies source stock availability.
     - Deducts transfer quantity from the source branch stock.
     - Adds transfer quantity to the destination branch stock (creating a stock record if the destination didn't have one).
     - Marks request status as `COMPLETED`.
     - Automatically checks if source stock has fallen below the product's minimum threshold!

### Automated Low-Stock Purchase Request
- After any stock-reducing operation (such as completing an inter-branch transfer), the service checks whether `currentQuantity < minimumStockThreshold`.
- If stock is below threshold, it checks whether a `PENDING` purchase request already exists for that branch and product.
- If none exists, it automatically generates a `PENDING` purchase request with a sensible restock quantity: `threshold - currentQuantity`.
- Duplicate pending purchase requests are strictly prevented.
- Administrators can review, approve, or reject purchase requests from the Purchase Requests tab.

### Safe Deletion Enforcement
- Deleting a branch or product is blocked if active inventory stock records, assigned staff members, or historical transfer/purchase requests depend on it.
- Rejection responses return descriptive 409 Conflict / 400 Bad Request messages so students and users understand exactly why an item cannot be deleted.

---

## 9. REST API Reference Summary

| Method | Endpoint | Access | Description |
|---|---|:---:|---|
| `POST` | `/api/auth/register` | Public | Register new staff member (enforces STAFF role) |
| `POST` | `/api/auth/login` | Public | Authenticate user and receive JWT token |
| `GET` | `/api/auth/me` | Authenticated | Fetch current user profile & assigned branch |
| `GET` | `/api/branches` | Authenticated | List all warehouse branches |
| `POST` | `/api/branches` | ADMIN | Create a new branch |
| `PUT` | `/api/branches/{id}` | ADMIN | Update branch details |
| `DELETE` | `/api/branches/{id}` | ADMIN | Delete branch (protected by safe deletion checks) |
| `GET` | `/api/products` | Authenticated | List all catalog products |
| `POST` | `/api/products` | ADMIN | Create new product (validates unique SKU) |
| `PUT` | `/api/products/{id}` | ADMIN | Update product |
| `DELETE` | `/api/products/{id}` | ADMIN | Delete product (safe deletion protected) |
| `GET` | `/api/stocks` | Authenticated | Multi-branch stock view (`?branchId=` optional) |
| `POST` | `/api/stocks/initialize` | ADMIN | Initialize stock for a branch/product combination |
| `GET` | `/api/transfers` | Authenticated | List transfer requests (role-filtered) |
| `POST` | `/api/transfers` | STAFF | Create transfer request from assigned branch |
| `PUT` | `/api/transfers/{id}/approve` | ADMIN | Mark transfer request as APPROVED |
| `PUT` | `/api/transfers/{id}/reject` | ADMIN | Mark transfer request as REJECTED |
| `PUT` | `/api/transfers/{id}/complete` | ADMIN | Atomically execute transfer & update both stocks |
| `GET` | `/api/purchases` | Authenticated | View automatic purchase requests |
| `PUT` | `/api/purchases/{id}/approve` | ADMIN | Approve purchase request |
| `PUT` | `/api/purchases/{id}/reject` | ADMIN | Reject purchase request |
| `GET` | `/api/users/staff` | ADMIN | List all registered staff accounts |
| `PUT` | `/api/users/staff/{id}/branch` | ADMIN | Assign/unassign warehouse branch to staff member |
