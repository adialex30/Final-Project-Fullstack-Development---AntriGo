-- AntriGo core schema — 10 tabel inti
SET NAMES utf8mb4;

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120)    NOT NULL,
    email           VARCHAR(160)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120)    NOT NULL,
    slug            VARCHAR(140)    NOT NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT          NOT NULL,
    name            VARCHAR(160)    NOT NULL,
    slug            VARCHAR(180)    NOT NULL,
    description     VARCHAR(500)    NULL,
    price           DECIMAL(12,2)   NOT NULL,
    stock           INT             NOT NULL DEFAULT 0 COMMENT 'cache — sumber kebenaran ada di stock_movements',
    image_url       VARCHAR(500)    NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_products_slug (slug),
    KEY idx_products_category_active (category_id, is_active),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_variants (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    variant_group   VARCHAR(60)     NOT NULL COMMENT 'e.g. SIZE, SPICE_LEVEL',
    name            VARCHAR(120)    NOT NULL COMMENT 'e.g. Besar, Level 3',
    price_delta     DECIMAL(12,2)   NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_variants_product (product_id),
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number            VARCHAR(40)     NOT NULL COMMENT 'opaque public reference, e.g. ORD-20260808-A1B2C3',
    business_date           DATE            NOT NULL COMMENT 'hari operasional, dasar reset nomor antrean',
    queue_number             INT             NOT NULL,
    table_number            VARCHAR(20)     NULL,
    status                  ENUM('QUEUED','PROCESSING','READY','COMPLETED','CANCELLED') NOT NULL DEFAULT 'QUEUED',
    subtotal_amount         DECIMAL(12,2)   NOT NULL,
    total_amount            DECIMAL(12,2)   NOT NULL,
    estimated_wait_minutes  INT             NOT NULL DEFAULT 0,
    note                    VARCHAR(300)    NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_orders_number (order_number),
    UNIQUE KEY uk_orders_business_date_queue (business_date, queue_number),
    KEY idx_orders_status_date (business_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id                BIGINT          NOT NULL,
    product_id              BIGINT          NOT NULL,
    variant_id              BIGINT          NULL,
    product_name_snapshot   VARCHAR(160)    NOT NULL,
    variant_name_snapshot   VARCHAR(120)    NULL,
    unit_price_snapshot     DECIMAL(12,2)   NOT NULL,
    quantity                INT             NOT NULL,
    line_total              DECIMAL(12,2)   NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    method          ENUM('QRIS','CASH') NOT NULL,
    status          ENUM('PENDING','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    amount          DECIMAL(12,2)   NOT NULL,
    paid_at         TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payments_order (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_status_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    from_status         VARCHAR(20)     NULL,
    to_status           VARCHAR(20)     NOT NULL,
    changed_by_user_id  BIGINT          NULL,
    note                VARCHAR(300)    NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_status_logs_order (order_id),
    CONSTRAINT fk_status_logs_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_status_logs_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_movements (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT          NOT NULL,
    type                ENUM('IN','OUT','ADJUSTMENT','CANCELLATION_REVERSAL') NOT NULL,
    quantity_change     INT             NOT NULL COMMENT 'positif = menambah stok, negatif = mengurangi',
    reference_type      VARCHAR(30)     NULL COMMENT 'ORDER, MANUAL, RESTOCK',
    reference_id        BIGINT          NULL,
    note                VARCHAR(300)    NULL,
    created_by_user_id  BIGINT          NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_stock_movements_product_date (product_id, created_at),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store_settings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key     VARCHAR(80)     NOT NULL,
    setting_value   VARCHAR(300)    NOT NULL,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
