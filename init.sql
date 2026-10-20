-- ==============================================================
-- Multi-Branch Warehouse Inventory Management System
-- Database Creation Script
-- ==============================================================

CREATE DATABASE IF NOT EXISTS warehouse_inventory
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE warehouse_inventory;

-- Tables are automatically managed and updated by Spring Boot JPA (ddl-auto=update).
-- The application automatically boots a default Administrator account and initial demonstration data if tables are empty.
