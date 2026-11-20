-- Full MySQL schema for Business Manager Sync + RBAC + Themes (Hostinger Ready)

CREATE TABLE IF NOT EXISTS `roles` (
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NOT NULL DEFAULT '',
  `permissions_json` TEXT NOT NULL,
  `is_system` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at_ms` BIGINT NOT NULL DEFAULT 0,
  `updated_at_ms` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_users` (
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(150) NOT NULL,
  `email` VARCHAR(191) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role_id` CHAR(36) NOT NULL,
  `business_ids_json` TEXT NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `last_login_ms` BIGINT NOT NULL DEFAULT 0,
  `created_at_ms` BIGINT NOT NULL DEFAULT 0,
  `updated_at_ms` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_app_users_email` (`email`),
  INDEX `idx_app_users_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_user_tokens` (
  `id` CHAR(36) NOT NULL,
  `user_id` CHAR(36) NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `device_id` VARCHAR(100) NOT NULL DEFAULT '',
  `created_at_ms` BIGINT NOT NULL DEFAULT 0,
  `last_used_ms` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tokens_hash` (`token_hash`),
  INDEX `idx_tokens_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `businesses` (
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `tagline` VARCHAR(255) NOT NULL DEFAULT '',
  `phone` VARCHAR(255) NOT NULL DEFAULT '',
  `email` VARCHAR(255) NOT NULL DEFAULT '',
  `address` TEXT NULL,
  `tax_id` VARCHAR(255) NOT NULL DEFAULT '',
  `invoice_prefix` VARCHAR(16) NOT NULL DEFAULT 'BM',
  `theme_key` VARCHAR(32) NOT NULL DEFAULT 'OCEAN',
  `default_tax_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  `invoice_footer` TEXT NULL,
  `created_at_ms` BIGINT NOT NULL DEFAULT 0,
  `updated_at_ms` BIGINT NOT NULL DEFAULT 0,
  `server_modified_at_ms` BIGINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_businesses_updated` (`updated_at_ms`),
  INDEX `idx_businesses_server_mod` (`server_modified_at_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `products` (
  `id` CHAR(36) NOT NULL,
  `business_id` CHAR(36) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `brand` VARCHAR(255) NOT NULL DEFAULT '',
  `category` VARCHAR(64) NOT NULL DEFAULT 'General',
  `pack_size` VARCHAR(64) NOT NULL DEFAULT '',
  `barcode` VARCHAR(128) NOT NULL DEFAULT '',
  `batch_number` VARCHAR(128) NOT NULL DEFAULT '',
  `expiry_date_millis` BIGINT NOT NULL DEFAULT 0,
  `stock_qty` INT NOT NULL DEFAULT 0,
  `low_stock_threshold` INT NOT NULL DEFAULT 10,
  `purchase_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `mrp_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `distributor_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `wholesaler_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `retail_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `performed_by_id` CHAR(36) NOT NULL DEFAULT '',
  `performed_by_name` VARCHAR(150) NOT NULL DEFAULT '',
  `updated_at_ms` BIGINT NOT NULL DEFAULT 0,
  `server_modified_at_ms` BIGINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_products_business` (`business_id`),
  INDEX `idx_products_barcode` (`barcode`),
  INDEX `idx_products_updated` (`updated_at_ms`),
  INDEX `idx_products_server_mod` (`server_modified_at_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `invoices` (
  `id` CHAR(36) NOT NULL,
  `business_id` CHAR(36) NOT NULL,
  `invoice_number` VARCHAR(64) NOT NULL,
  `customer_name` VARCHAR(255) NOT NULL,
  `customer_phone` VARCHAR(64) NOT NULL DEFAULT '',
  `customer_email` VARCHAR(255) NOT NULL DEFAULT '',
  `pricing_tier` VARCHAR(32) NOT NULL DEFAULT 'RETAIL',
  `subtotal` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `tax_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `tax_rate_percent` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `grand_total` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `payment_method` VARCHAR(64) NOT NULL DEFAULT 'Cash',
  `payment_status` VARCHAR(32) NOT NULL DEFAULT 'PAID',
  `amount_paid` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `balance_due` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `is_returned` TINYINT(1) NOT NULL DEFAULT 0,
  `notes` TEXT NULL,
  `performed_by_id` CHAR(36) NOT NULL DEFAULT '',
  `performed_by_name` VARCHAR(150) NOT NULL DEFAULT '',
  `created_at_ms` BIGINT NOT NULL DEFAULT 0,
  `updated_at_ms` BIGINT NOT NULL DEFAULT 0,
  `server_modified_at_ms` BIGINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_invoices_business` (`business_id`),
  INDEX `idx_invoices_number` (`invoice_number`),
  INDEX `idx_invoices_updated` (`updated_at_ms`),
  INDEX `idx_invoices_server_mod` (`server_modified_at_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `invoice_items` (
  `id` CHAR(36) NOT NULL,
  `invoice_id` CHAR(36) NOT NULL,
  `product_id` CHAR(36) NOT NULL DEFAULT '',
  `product_name` VARCHAR(255) NOT NULL,
  `pack_size` VARCHAR(64) NOT NULL DEFAULT '',
  `batch_number` VARCHAR(128) NOT NULL DEFAULT '',
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `line_total` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  INDEX `idx_invoice_items_invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `expenses` (
  `id` CHAR(36) NOT NULL,
  `business_id` CHAR(36) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `category` VARCHAR(64) NOT NULL DEFAULT 'Other',
  `amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `payment_method` VARCHAR(64) NOT NULL DEFAULT 'Cash',
  `payee` VARCHAR(255) NOT NULL DEFAULT '',
  `notes` TEXT NULL,
  `performed_by_id` CHAR(36) NOT NULL DEFAULT '',
  `performed_by_name` VARCHAR(150) NOT NULL DEFAULT '',
  `expense_date_millis` BIGINT NOT NULL DEFAULT 0,
  `updated_at_ms` BIGINT NOT NULL DEFAULT 0,
  `server_modified_at_ms` BIGINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_expenses_business` (`business_id`),
  INDEX `idx_expenses_updated` (`updated_at_ms`),
  INDEX `idx_expenses_server_mod` (`server_modified_at_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
