/* Multi-Branch Warehouse Inventory Management - script.js */

const API_BASE = "/api";
const TOKEN_KEY = "warehouse_jwt_token";
const USER_KEY = "warehouse_user_data";

let currentUser = null;
let cachedBranches = [];
let cachedProducts = [];

// ================= INITIALIZATION & AUTH =================

document.addEventListener("DOMContentLoaded", async () => {
    const isAuthPage = document.getElementById("login-form") !== null;
    const isDashboardPage = document.getElementById("dashboard-page") !== null;
    const isManagementPage = document.getElementById("management-page") !== null;
    const isTransfersPage = document.getElementById("transfers-page") !== null;
    const isPurchasesPage = document.getElementById("purchases-page") !== null;

    const authenticated = await checkAuthState();

    // 1. Auth Page (index.html) behavior
    if (isAuthPage) {
        if (authenticated) {
            window.location.href = "dashboard.html";
            return;
        }
        return;
    }

    // 2. Protected Pages: If not logged in, redirect to login page
    if (!authenticated) {
        window.location.href = "index.html";
        return;
    }

    // 3. Admin-only Management page protection
    if (isManagementPage && currentUser.role !== "ADMIN") {
        alert("Access denied: Administrative Management is restricted to Administrators.");
        window.location.href = "dashboard.html";
        return;
    }

    // 4. Update Header UI elements
    updateHeaderUI();

    // 5. Page-specific data initialization
    if (isDashboardPage) {
        await Promise.all([loadBranches(), loadProducts()]);
        loadDashboardAnalytics();
        loadStocks();
    } else if (isManagementPage) {
        await Promise.all([loadBranches(), loadProducts()]);
        loadStaffList();
    } else if (isTransfersPage) {
        await Promise.all([loadBranches(), loadProducts()]);
        loadTransfers();
    } else if (isPurchasesPage) {
        await loadBranches();
        loadPurchases();
    }
});

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function setAuth(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    currentUser = user;
}

function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    currentUser = null;
}

async function apiFetch(endpoint, options = {}) {
    const token = getToken();
    const headers = options.headers || {};

    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    if (options.body && typeof options.body === "object" && !(options.body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(options.body);
    }

    options.headers = headers;

    const response = await fetch(`${API_BASE}${endpoint}`, options);

    if (response.status === 401) {
        clearAuth();
        const isAuthPage = document.getElementById("login-form") !== null;
        if (!isAuthPage) {
            window.location.href = "index.html";
        }
        throw new Error("Unauthorized or session expired");
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
        const errorMsg = data && (data.message || data.error) ? (data.message || data.error) : `Error: ${response.statusText}`;
        throw new Error(errorMsg);
    }

    return data;
}

async function checkAuthState() {
    const token = getToken();
    if (!token) {
        return false;
    }

    try {
        const user = await apiFetch("/auth/me");
        currentUser = user;
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        return true;
    } catch (e) {
        clearAuth();
        return false;
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById("login-email").value.trim();
    const password = document.getElementById("login-password").value;

    try {
        const result = await apiFetch("/auth/login", {
            method: "POST",
            body: { email, password }
        });

        const user = {
            id: result.userId,
            name: result.name,
            email: result.email,
            role: result.role,
            assignedBranchId: result.assignedBranchId,
            assignedBranchName: result.assignedBranchName
        };

        setAuth(result.token, user);
        window.location.href = "dashboard.html";
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById("register-name").value.trim();
    const email = document.getElementById("register-email").value.trim();
    const password = document.getElementById("register-password").value;

    try {
        const res = await apiFetch("/auth/register", {
            method: "POST",
            body: { name, email, password }
        });

        showAlert(res.message || "Registration successful! You may now sign in.", "success");
        document.getElementById("register-form").reset();
        document.getElementById("login-email").value = email;
        document.getElementById("login-password").focus();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

function logout() {
    clearAuth();
    window.location.href = "index.html";
}

function updateHeaderUI() {
    if (!currentUser) return;

    const displayName = document.getElementById("user-display-name");
    const roleBadge = document.getElementById("user-role-badge");
    const branchTag = document.getElementById("user-branch-indicator");

    if (displayName) displayName.textContent = currentUser.name;
    if (roleBadge) roleBadge.textContent = currentUser.role;

    if (branchTag) {
        if (currentUser.role === "ADMIN") {
            branchTag.textContent = "Branch: All (Admin)";
        } else {
            branchTag.textContent = currentUser.assignedBranchName ? `Branch: ${currentUser.assignedBranchName}` : "Branch: Unassigned";
        }
    }

    const isAdmin = currentUser.role === "ADMIN";

    document.querySelectorAll(".admin-only").forEach(el => {
        if (isAdmin) {
            el.classList.remove("hidden");
        } else {
            el.classList.add("hidden");
        }
    });

    document.querySelectorAll(".staff-only").forEach(el => {
        if (!isAdmin) {
            el.classList.remove("hidden");
        } else {
            el.classList.add("hidden");
        }
    });

    // Update staff transfer card status if on transfers page
    if (!isAdmin) {
        const unassignedNotice = document.getElementById("staff-unassigned-notice");
        const submitBtn = document.getElementById("btn-submit-transfer");
        const sourceDisplay = document.getElementById("transfer-source-display");

        if (unassignedNotice && submitBtn && sourceDisplay) {
            if (currentUser.assignedBranchId) {
                unassignedNotice.classList.add("hidden");
                submitBtn.disabled = false;
                sourceDisplay.value = currentUser.assignedBranchName;
            } else {
                unassignedNotice.classList.remove("hidden");
                submitBtn.disabled = true;
                sourceDisplay.value = "Unassigned (Contact Admin)";
            }
        }
    }
}

function showAlert(message, type = "success") {
    const box = document.getElementById("alert-box");
    if (!box) return;

    box.className = `alert alert-${type}`;
    box.textContent = message;
    box.classList.remove("hidden");

    window.scrollTo({ top: 0, behavior: "smooth" });

    setTimeout(() => {
        box.classList.add("hidden");
    }, 6000);
}

// ================= 1. DASHBOARD ANALYTICS =================

async function loadDashboardAnalytics() {
    try {
        const [branches, products, stocks, transfers, purchases] = await Promise.all([
            apiFetch("/branches").catch(() => []),
            apiFetch("/products").catch(() => []),
            apiFetch("/stocks").catch(() => []),
            apiFetch("/transfers").catch(() => []),
            apiFetch("/purchases").catch(() => [])
        ]);

        const totalBranches = branches ? branches.length : 0;
        const totalProducts = products ? products.length : 0;
        const lowStockCount = stocks ? stocks.filter(s => s.status === "LOW_STOCK").length : 0;
        const pendingTransfers = transfers ? transfers.filter(t => t.status === "PENDING").length : 0;
        const pendingPurchases = purchases ? purchases.filter(p => p.status === "PENDING").length : 0;

        const elBranches = document.getElementById("stat-branches");
        const elProducts = document.getElementById("stat-products");
        const elLowStock = document.getElementById("stat-low-stock");
        const elPendingTransfers = document.getElementById("stat-pending-transfers");
        const elPendingPurchases = document.getElementById("stat-pending-purchases");

        if (elBranches) elBranches.textContent = totalBranches;
        if (elProducts) elProducts.textContent = totalProducts;
        if (elLowStock) elLowStock.textContent = lowStockCount;
        if (elPendingTransfers) elPendingTransfers.textContent = pendingTransfers;
        if (elPendingPurchases) elPendingPurchases.textContent = pendingPurchases;
    } catch (e) {
        console.error("Failed to calculate dashboard analytics:", e);
    }
}

// ================= 2. STOCK OVERVIEW =================

async function loadStocks() {
    const filterEl = document.getElementById("stock-branch-filter");
    if (!filterEl) return;

    const branchFilter = filterEl.value;
    const url = branchFilter ? `/stocks?branchId=${branchFilter}` : "/stocks";

    const tbody = document.getElementById("stock-table-body");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="6" class="text-center">Loading stock data...</td></tr>`;

    try {
        const stocks = await apiFetch(url);
        if (!stocks || stocks.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center">No stock records found.</td></tr>`;
            return;
        }

        tbody.innerHTML = stocks.map(s => {
            const isLow = s.status === "LOW_STOCK";
            const badgeClass = isLow ? "badge-low-stock" : "badge-in-stock";
            const statusLabel = isLow ? "⚠️ Low Stock" : "✓ In Stock";

            return `
                <tr>
                    <td><strong>${escapeHtml(s.branchName)}</strong></td>
                    <td>${escapeHtml(s.productName)}</td>
                    <td><code>${escapeHtml(s.productSku)}</code></td>
                    <td><strong>${s.quantity}</strong></td>
                    <td>${s.minimumStockThreshold}</td>
                    <td><span class="badge ${badgeClass}">${statusLabel}</span></td>
                </tr>
            `;
        }).join("");
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Failed to load stock data: ${escapeHtml(e.message)}</td></tr>`;
    }
}

async function handleInitStock(e) {
    e.preventDefault();
    const branchId = document.getElementById("init-branch-select").value;
    const productId = document.getElementById("init-product-select").value;
    const quantity = document.getElementById("init-quantity").value;

    try {
        await apiFetch("/stocks/initialize", {
            method: "POST",
            body: {
                branchId: parseInt(branchId),
                productId: parseInt(productId),
                quantity: parseInt(quantity)
            }
        });

        showAlert("Branch stock initialized successfully!", "success");
        document.getElementById("init-stock-form").reset();
        loadStocks();
        loadDashboardAnalytics();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ================= 3. TRANSFERS =================

async function loadTransfers() {
    const tbody = document.getElementById("transfer-table-body");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="9" class="text-center">Loading transfers...</td></tr>`;

    try {
        const transfers = await apiFetch("/transfers");
        if (!transfers || transfers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center">No transfer requests found.</td></tr>`;
            return;
        }

        const isAdmin = currentUser && currentUser.role === "ADMIN";

        tbody.innerHTML = transfers.map(t => {
            let badgeClass = "badge-pending";
            if (t.status === "APPROVED") badgeClass = "badge-approved";
            if (t.status === "REJECTED") badgeClass = "badge-rejected";
            if (t.status === "COMPLETED") badgeClass = "badge-completed";

            let actionsHtml = "";
            if (isAdmin) {
                if (t.status === "PENDING") {
                    actionsHtml = `
                        <div class="btn-group">
                            <button class="btn btn-sm btn-success" onclick="approveTransfer(${t.id})">Approve</button>
                            <button class="btn btn-sm btn-danger" onclick="rejectTransfer(${t.id})">Reject</button>
                        </div>
                    `;
                } else if (t.status === "APPROVED") {
                    actionsHtml = `
                        <div class="btn-group">
                            <button class="btn btn-sm btn-primary" onclick="completeTransfer(${t.id})">Complete Transfer</button>
                            <button class="btn btn-sm btn-danger" onclick="rejectTransfer(${t.id})">Reject</button>
                        </div>
                    `;
                } else {
                    actionsHtml = `<span class="text-muted">None</span>`;
                }
            }

            const formattedDate = t.createdAt ? new Date(t.createdAt).toLocaleString() : "-";

            return `
                <tr>
                    <td>#${t.id}</td>
                    <td>${escapeHtml(t.sourceBranch.name)}</td>
                    <td>${escapeHtml(t.destinationBranch.name)}</td>
                    <td>${escapeHtml(t.product.name)} (<code>${escapeHtml(t.product.sku)}</code>)</td>
                    <td><strong>${t.quantity}</strong></td>
                    <td>${escapeHtml(t.requestedBy.name)}</td>
                    <td><span class="badge ${badgeClass}">${t.status}</span></td>
                    <td><small>${formattedDate}</small></td>
                    ${isAdmin ? `<td>${actionsHtml}</td>` : ""}
                </tr>
            `;
        }).join("");
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger">Failed to load transfers: ${escapeHtml(e.message)}</td></tr>`;
    }
}

async function handleCreateTransfer(e) {
    e.preventDefault();
    const destBranchId = document.getElementById("transfer-dest-select").value;
    const productId = document.getElementById("transfer-product-select").value;
    const quantity = document.getElementById("transfer-quantity").value;

    try {
        await apiFetch("/transfers", {
            method: "POST",
            body: {
                destinationBranchId: parseInt(destBranchId),
                productId: parseInt(productId),
                quantity: parseInt(quantity)
            }
        });

        showAlert("Transfer request created successfully! It is now PENDING Administrator approval.", "success");
        document.getElementById("transfer-create-form").reset();
        document.getElementById("transfer-source-display").value = currentUser.assignedBranchName;
        loadTransfers();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function approveTransfer(id) {
    try {
        await apiFetch(`/transfers/${id}/approve`, { method: "PUT" });
        showAlert(`Transfer #${id} approved! You can now complete the transfer when ready.`, "success");
        loadTransfers();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function rejectTransfer(id) {
    if (!confirm(`Are you sure you want to reject transfer request #${id}?`)) return;
    try {
        await apiFetch(`/transfers/${id}/reject`, { method: "PUT" });
        showAlert(`Transfer #${id} rejected. No stock was modified.`, "info");
        loadTransfers();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function completeTransfer(id) {
    try {
        await apiFetch(`/transfers/${id}/complete`, { method: "PUT" });
        showAlert(`Transfer #${id} completed successfully! Stock updated at both branches.`, "success");
        loadTransfers();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ================= 4. PURCHASES =================

async function loadPurchases() {
    const filterEl = document.getElementById("purchase-branch-filter");
    const branchFilter = filterEl ? filterEl.value : "";
    const url = branchFilter ? `/purchases?branchId=${branchFilter}` : "/purchases";

    const tbody = document.getElementById("purchase-table-body");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="7" class="text-center">Loading purchase requests...</td></tr>`;

    try {
        const purchases = await apiFetch(url);
        if (!purchases || purchases.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center">No purchase requests found.</td></tr>`;
            return;
        }

        const isAdmin = currentUser && currentUser.role === "ADMIN";

        tbody.innerHTML = purchases.map(p => {
            let badgeClass = "badge-pending";
            if (p.status === "APPROVED") badgeClass = "badge-approved";
            if (p.status === "REJECTED") badgeClass = "badge-rejected";

            let actionsHtml = "";
            if (isAdmin) {
                if (p.status === "PENDING") {
                    actionsHtml = `
                        <div class="btn-group">
                            <button class="btn btn-sm btn-success" onclick="approvePurchase(${p.id})">Approve</button>
                            <button class="btn btn-sm btn-danger" onclick="rejectPurchase(${p.id})">Reject</button>
                        </div>
                    `;
                } else {
                    actionsHtml = `<span class="text-muted">None</span>`;
                }
            }

            const formattedDate = p.createdAt ? new Date(p.createdAt).toLocaleString() : "-";

            return `
                <tr>
                    <td>#${p.id}</td>
                    <td><strong>${escapeHtml(p.branch.name)}</strong></td>
                    <td>${escapeHtml(p.product.name)} (<code>${escapeHtml(p.product.sku)}</code>)</td>
                    <td><strong>${p.requestedQuantity}</strong></td>
                    <td><span class="badge ${badgeClass}">${p.status}</span></td>
                    <td><small>${formattedDate}</small></td>
                    ${isAdmin ? `<td>${actionsHtml}</td>` : ""}
                </tr>
            `;
        }).join("");
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Failed to load purchase requests: ${escapeHtml(e.message)}</td></tr>`;
    }
}

async function approvePurchase(id) {
    try {
        await apiFetch(`/purchases/${id}/approve`, { method: "PUT" });
        showAlert(`Purchase request #${id} marked as APPROVED.`, "success");
        loadPurchases();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function rejectPurchase(id) {
    if (!confirm(`Are you sure you want to reject purchase request #${id}?`)) return;
    try {
        await apiFetch(`/purchases/${id}/reject`, { method: "PUT" });
        showAlert(`Purchase request #${id} marked as REJECTED.`, "info");
        loadPurchases();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ================= 5. MANAGEMENT (BRANCHES, PRODUCTS, STAFF) =================

function switchMgmtTab(sectionId) {
    document.querySelectorAll(".sub-nav-btn").forEach(btn => {
        const target = btn.getAttribute("onclick");
        btn.classList.toggle("active", target && target.includes(sectionId));
    });

    document.querySelectorAll(".mgmt-section").forEach(sec => {
        sec.classList.toggle("hidden", sec.id !== sectionId);
    });
}

async function loadBranches() {
    try {
        const branches = await apiFetch("/branches");
        cachedBranches = branches || [];
        populateBranchDropdowns(cachedBranches);

        const tbody = document.getElementById("branch-table-body");
        if (!tbody) return;

        if (cachedBranches.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center">No branches configured.</td></tr>`;
            return;
        }

        tbody.innerHTML = cachedBranches.map(b => `
            <tr>
                <td>#${b.id}</td>
                <td><strong>${escapeHtml(b.name)}</strong></td>
                <td>${escapeHtml(b.location)}</td>
                <td>
                    <div class="btn-group">
                        <button class="btn btn-sm btn-secondary" onclick="editBranch(${b.id}, '${escapeJs(b.name)}', '${escapeJs(b.location)}')">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteBranch(${b.id})">Delete</button>
                    </div>
                </td>
            </tr>
        `).join("");
    } catch (e) {
        // Handled silently if on page where branches endpoint is secondary
    }
}

function populateBranchDropdowns(branches) {
    // 1. Stock filter dropdown (dashboard)
    const stockFilter = document.getElementById("stock-branch-filter");
    if (stockFilter) {
        const currentVal = stockFilter.value;
        stockFilter.innerHTML = `<option value="">All Branches</option>` + branches.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
        stockFilter.value = currentVal;
    }

    // 2. Purchase filter dropdown (purchases)
    const purchaseFilter = document.getElementById("purchase-branch-filter");
    if (purchaseFilter) {
        const currentVal = purchaseFilter.value;
        purchaseFilter.innerHTML = `<option value="">All Branches</option>` + branches.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
        purchaseFilter.value = currentVal;
    }

    // 3. Transfer destination dropdown (transfers)
    const transferDest = document.getElementById("transfer-dest-select");
    if (transferDest) {
        const userBranchId = currentUser && currentUser.assignedBranchId;
        const eligibleDestBranches = branches.filter(b => b.id !== userBranchId);
        transferDest.innerHTML = `<option value="">Select Destination Branch</option>` + eligibleDestBranches.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
    }

    // 4. Init stock branch dropdown (dashboard)
    const initBranch = document.getElementById("init-branch-select");
    if (initBranch) {
        initBranch.innerHTML = `<option value="">Select Branch</option>` + branches.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
    }
}

async function handleSaveBranch(e) {
    e.preventDefault();
    const id = document.getElementById("branch-id").value;
    const name = document.getElementById("branch-name").value.trim();
    const location = document.getElementById("branch-location").value.trim();

    try {
        if (id) {
            await apiFetch(`/branches/${id}`, {
                method: "PUT",
                body: { name, location }
            });
            showAlert("Branch updated successfully!", "success");
        } else {
            await apiFetch("/branches", {
                method: "POST",
                body: { name, location }
            });
            showAlert("Branch created successfully!", "success");
        }

        cancelBranchEdit();
        loadBranches();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

function editBranch(id, name, location) {
    document.getElementById("branch-id").value = id;
    document.getElementById("branch-name").value = name;
    document.getElementById("branch-location").value = location;
    document.getElementById("branch-form-title").textContent = "Edit Branch #" + id;
    document.getElementById("btn-save-branch").textContent = "Update Branch";
    document.getElementById("btn-cancel-branch").classList.remove("hidden");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function cancelBranchEdit() {
    const idEl = document.getElementById("branch-id");
    if (idEl) idEl.value = "";
    const form = document.getElementById("branch-form");
    if (form) form.reset();
    const title = document.getElementById("branch-form-title");
    if (title) title.textContent = "Add New Branch";
    const saveBtn = document.getElementById("btn-save-branch");
    if (saveBtn) saveBtn.textContent = "Save Branch";
    const cancelBtn = document.getElementById("btn-cancel-branch");
    if (cancelBtn) cancelBtn.classList.add("hidden");
}

async function deleteBranch(id) {
    if (!confirm(`Are you sure you want to delete Branch #${id}? Deletion will only succeed if no stock, staff, or requests depend on it.`)) {
        return;
    }

    try {
        const res = await apiFetch(`/branches/${id}`, { method: "DELETE" });
        showAlert(res.message || "Branch deleted successfully", "success");
        loadBranches();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function loadProducts() {
    try {
        const products = await apiFetch("/products");
        cachedProducts = products || [];
        populateProductDropdowns(cachedProducts);

        const tbody = document.getElementById("product-table-body");
        if (!tbody) return;

        if (cachedProducts.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center">No products cataloged.</td></tr>`;
            return;
        }

        tbody.innerHTML = cachedProducts.map(p => `
            <tr>
                <td>#${p.id}</td>
                <td><strong>${escapeHtml(p.name)}</strong></td>
                <td><code>${escapeHtml(p.sku)}</code></td>
                <td>${p.minimumStockThreshold}</td>
                <td><small>${escapeHtml(p.description || "-")}</small></td>
                <td>
                    <div class="btn-group">
                        <button class="btn btn-sm btn-secondary" onclick="editProduct(${p.id}, '${escapeJs(p.name)}', '${escapeJs(p.sku)}', ${p.minimumStockThreshold}, '${escapeJs(p.description || "")}')">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteProduct(${p.id})">Delete</button>
                    </div>
                </td>
            </tr>
        `).join("");
    } catch (e) {
        // Handled silently
    }
}

function populateProductDropdowns(products) {
    const transferProduct = document.getElementById("transfer-product-select");
    if (transferProduct) {
        transferProduct.innerHTML = `<option value="">Select Product</option>` + products.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (${escapeHtml(p.sku)})</option>`).join("");
    }

    const initProduct = document.getElementById("init-product-select");
    if (initProduct) {
        initProduct.innerHTML = `<option value="">Select Product</option>` + products.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (${escapeHtml(p.sku)})</option>`).join("");
    }
}

async function handleSaveProduct(e) {
    e.preventDefault();
    const id = document.getElementById("product-id").value;
    const name = document.getElementById("product-name").value.trim();
    const sku = document.getElementById("product-sku").value.trim();
    const threshold = document.getElementById("product-threshold").value;
    const desc = document.getElementById("product-desc").value.trim();

    try {
        const body = {
            name,
            sku,
            minimumStockThreshold: parseInt(threshold),
            description: desc
        };

        if (id) {
            await apiFetch(`/products/${id}`, {
                method: "PUT",
                body
            });
            showAlert("Product updated successfully!", "success");
        } else {
            await apiFetch("/products", {
                method: "POST",
                body
            });
            showAlert("Product created successfully!", "success");
        }

        cancelProductEdit();
        loadProducts();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

function editProduct(id, name, sku, threshold, description) {
    document.getElementById("product-id").value = id;
    document.getElementById("product-name").value = name;
    document.getElementById("product-sku").value = sku;
    document.getElementById("product-threshold").value = threshold;
    document.getElementById("product-desc").value = description;

    document.getElementById("product-form-title").textContent = "Edit Product #" + id;
    document.getElementById("btn-save-product").textContent = "Update Product";
    document.getElementById("btn-cancel-product").classList.remove("hidden");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function cancelProductEdit() {
    const idEl = document.getElementById("product-id");
    if (idEl) idEl.value = "";
    const form = document.getElementById("product-form");
    if (form) form.reset();
    const title = document.getElementById("product-form-title");
    if (title) title.textContent = "Add New Product";
    const saveBtn = document.getElementById("btn-save-product");
    if (saveBtn) saveBtn.textContent = "Save Product";
    const cancelBtn = document.getElementById("btn-cancel-product");
    if (cancelBtn) cancelBtn.classList.add("hidden");
}

async function deleteProduct(id) {
    if (!confirm(`Are you sure you want to delete Product #${id}? Deletion will only succeed if no stock or requests reference it.`)) {
        return;
    }

    try {
        const res = await apiFetch(`/products/${id}`, { method: "DELETE" });
        showAlert(res.message || "Product deleted successfully", "success");
        loadProducts();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function loadStaffList() {
    const tbody = document.getElementById("staff-table-body");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="5" class="text-center">Loading staff members...</td></tr>`;

    try {
        const staffList = await apiFetch("/users/staff");
        if (!staffList || staffList.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center">No registered staff members found.</td></tr>`;
            return;
        }

        tbody.innerHTML = staffList.map(s => {
            const branchOptions = cachedBranches.map(b => {
                const selected = s.branchId === b.id ? "selected" : "";
                return `<option value="${b.id}" ${selected}>${escapeHtml(b.name)}</option>`;
            }).join("");

            const unassignSelected = !s.branchId ? "selected" : "";

            return `
                <tr>
                    <td>#${s.id}</td>
                    <td><strong>${escapeHtml(s.name)}</strong></td>
                    <td>${escapeHtml(s.email)}</td>
                    <td><span class="badge ${s.branchId ? "badge-approved" : "badge-pending"}">${escapeHtml(s.branchName)}</span></td>
                    <td>
                        <div class="form-row" style="align-items: center; margin-bottom: 0;">
                            <select id="staff-branch-select-${s.id}" style="width: auto; padding: 0.35rem 0.5rem; font-size: 0.85rem;">
                                <option value="" ${unassignSelected}>-- Unassign Branch --</option>
                                ${branchOptions}
                            </select>
                            <button class="btn btn-sm btn-primary" onclick="assignStaffBranch(${s.id})">Save Assignment</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join("");
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger">Failed to load staff list: ${escapeHtml(e.message)}</td></tr>`;
    }
}

async function assignStaffBranch(userId) {
    const select = document.getElementById(`staff-branch-select-${userId}`);
    const branchIdVal = select.value;
    const branchId = branchIdVal ? parseInt(branchIdVal) : null;

    try {
        await apiFetch(`/users/staff/${userId}/branch`, {
            method: "PUT",
            body: { branchId }
        });

        showAlert(`Branch assignment updated for Staff member #${userId}!`, "success");
        loadStaffList();
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ================= UTILITIES =================

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function escapeJs(str) {
    if (!str) return "";
    return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
}
