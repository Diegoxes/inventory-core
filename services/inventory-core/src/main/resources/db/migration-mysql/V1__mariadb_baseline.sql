-- MariaDB baseline — inventory-core (sin RBAC ni tablas de otros microservicios)

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    whatsapp_number VARCHAR(64),
    created_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS organizations (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    industry VARCHAR(255),
    currency VARCHAR(16) NOT NULL DEFAULT 'MXN',
    country VARCHAR(64),
    timezone VARCHAR(64),
    max_members INT NOT NULL DEFAULT 20,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS organization_settings (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL UNIQUE,
    expiry_alert_days INT NOT NULL DEFAULT 7,
    prediction_horizon_days INT NOT NULL DEFAULT 30,
    CONSTRAINT fk_org_settings_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS organization_members (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    org_role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_org_members_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS warehouses (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_warehouses_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    organization_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(64),
    internal_code VARCHAR(64),
    quantity DOUBLE NOT NULL DEFAULT 0,
    min_quantity DOUBLE NOT NULL DEFAULT 0,
    unit VARCHAR(32) NOT NULL DEFAULT 'UNIT',
    consumption_per_use DOUBLE,
    expiry_date DATE,
    barcode VARCHAR(100),
    category VARCHAR(255),
    image_url VARCHAR(512),
    sale_price DECIMAL(14,4),
    last_cost DECIMAL(14,4),
    avg_cost DECIMAL(14,4),
    purchase_unit VARCHAR(32),
    units_per_purchase_unit DOUBLE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_products_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_products_org (organization_id),
    INDEX idx_products_org_sku (organization_id, sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS suppliers (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    organization_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    lead_time_days INT,
    notes VARCHAR(2000),
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_suppliers_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_suppliers_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consumption_logs (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    quantity_change DOUBLE NOT NULL,
    action_type VARCHAR(32),
    source VARCHAR(32),
    note VARCHAR(500),
    measure_unit_id VARCHAR(255),
    input_quantity DOUBLE,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_consumption_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_consumption_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS purchases (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    supplier_id VARCHAR(255),
    quantity DOUBLE NOT NULL,
    unit_price DECIMAL(14,4),
    total_amount DECIMAL(14,2),
    currency VARCHAR(8),
    purchased_at TIMESTAMP NOT NULL,
    source VARCHAR(32),
    note VARCHAR(500),
    measure_unit_id VARCHAR(255),
    input_quantity DOUBLE,
    cost_input_mode VARCHAR(32),
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_purchases_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_purchases_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_aliases (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    alias_text VARCHAR(255) NOT NULL,
    normalized_alias VARCHAR(255) NOT NULL,
    learned_whatsapp TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_aliases_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_product_normalized_alias (product_id, normalized_alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color_hex VARCHAR(7),
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_categories_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    UNIQUE KEY uk_categories_org_name (organization_id, name),
    INDEX idx_categories_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stock_levels (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(255) NOT NULL,
    quantity DOUBLE NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory_lots (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(255) NOT NULL,
    quantity DOUBLE NOT NULL,
    expiry_date DATE,
    received_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lots_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255),
    user_id VARCHAR(255),
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(128),
    entity_id VARCHAR(255),
    channel VARCHAR(32),
    detail VARCHAR(2000),
    created_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS measure_units (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    base_unit TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_measure_units_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    UNIQUE KEY uq_measure_units_org_code (organization_id, code),
    INDEX idx_measure_units_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_uoms (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    measure_unit_id VARCHAR(255) NOT NULL,
    factor_to_base DOUBLE NOT NULL,
    CONSTRAINT fk_product_uoms_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_uoms_unit FOREIGN KEY (measure_unit_id) REFERENCES measure_units(id) ON DELETE CASCADE,
    UNIQUE KEY uq_product_uoms (product_id, measure_unit_id),
    INDEX idx_product_uoms_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
