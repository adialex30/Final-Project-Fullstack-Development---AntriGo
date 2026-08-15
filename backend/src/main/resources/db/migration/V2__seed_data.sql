-- Seed data supaya dashboard & katalog tidak terlihat kosong saat demo

-- Admin default: email admin@antrigo.id / password Admin12345
INSERT INTO users (name, email, password_hash, role) VALUES
('Admin AntriGo', 'admin@antrigo.id', '$2b$10$iI.vFzQAaIt/zigUbwqynOI5988nzzPeq.mCWJc60k2Yz9mt1yMfy', 'ADMIN'),
('Staff Dapur', 'staff@antrigo.id', '$2b$10$iI.vFzQAaIt/zigUbwqynOI5988nzzPeq.mCWJc60k2Yz9mt1yMfy', 'STAFF');

INSERT INTO categories (name, slug, sort_order) VALUES
('Makanan Utama', 'makanan-utama', 1),
('Minuman', 'minuman', 2),
('Camilan', 'camilan', 3);

INSERT INTO products (category_id, name, slug, description, price, stock, is_active) VALUES
(1, 'Nasi Goreng Spesial', 'nasi-goreng-spesial', 'Nasi goreng dengan telur, ayam suwir, dan acar', 22000, 40, TRUE),
(1, 'Mie Ayam Bakso', 'mie-ayam-bakso', 'Mie ayam dengan topping bakso sapi', 18000, 35, TRUE),
(1, 'Ayam Geprek Sambal Bawang', 'ayam-geprek-sambal-bawang', 'Ayam goreng crispy dengan sambal bawang', 20000, 25, TRUE),
(1, 'Soto Ayam Lamongan', 'soto-ayam-lamongan', 'Soto ayam kuah bening khas Lamongan', 19000, 1, TRUE),
(2, 'Es Teh Manis', 'es-teh-manis', 'Teh manis dingin segar', 5000, 100, TRUE),
(2, 'Es Jeruk Peras', 'es-jeruk-peras', 'Jeruk peras asli tanpa pemanis buatan', 8000, 60, TRUE),
(2, 'Kopi Susu Gula Aren', 'kopi-susu-gula-aren', 'Kopi susu dengan gula aren asli', 12000, 50, TRUE),
(3, 'Tahu Crispy', 'tahu-crispy', 'Tahu goreng tepung crispy dengan saus', 10000, 30, TRUE),
(3, 'Pisang Goreng Coklat Keju', 'pisang-goreng-coklat-keju', 'Pisang goreng topping coklat dan keju', 13000, 20, TRUE);

INSERT INTO product_variants (product_id, variant_group, name, price_delta) VALUES
(1, 'SPICE_LEVEL', 'Tidak Pedas', 0),
(1, 'SPICE_LEVEL', 'Pedas Sedang', 0),
(1, 'SPICE_LEVEL', 'Pedas Level 3', 0),
(3, 'SPICE_LEVEL', 'Level 1', 0),
(3, 'SPICE_LEVEL', 'Level 3', 0),
(3, 'SPICE_LEVEL', 'Level 5 (Extra Pedas)', 1000),
(7, 'SIZE', 'Regular', 0),
(7, 'SIZE', 'Large', 4000);

INSERT INTO store_settings (setting_key, setting_value) VALUES
('store_name', 'Warung AntriGo'),
('avg_prep_time_minutes', '4'),
('operating_hours', '08:00-21:00'),
('low_stock_threshold', '5');
