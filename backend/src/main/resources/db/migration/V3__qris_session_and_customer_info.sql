SET NAMES utf8mb4;

-- Status baru AWAITING_PAYMENT: order QRIS dibuat & sesi pembayaran (dummy Midtrans) berjalan,
-- tapi belum masuk antrean dapur. Order CASH tidak pernah singgah di status ini.
ALTER TABLE orders
    MODIFY COLUMN status ENUM('AWAITING_PAYMENT','QUEUED','PROCESSING','READY','COMPLETED','CANCELLED')
        NOT NULL DEFAULT 'QUEUED';

-- Nomor antrean baru digenerate SETELAH pembayaran QRIS dikonfirmasi, jadi boleh kosong sementara
-- selama order masih AWAITING_PAYMENT. UNIQUE(business_date, queue_number) tetap aman — MySQL
-- tidak menganggap NULL bentrok dengan NULL lain di unique index.
ALTER TABLE orders
    MODIFY COLUMN queue_number INT NULL;

-- Data pelanggan wajib diisi setiap checkout (nama untuk dipanggil saat pesanan siap, nomor
-- telepon sebagai identitas kontak). DEFAULT '' murni supaya ALTER tidak gagal kalau ada baris
-- lama di database dev — endpoint checkout sudah mewajibkan keduanya lewat validasi.
ALTER TABLE orders
    ADD COLUMN customer_name  VARCHAR(100) NOT NULL DEFAULT '' AFTER table_number,
    ADD COLUMN customer_phone VARCHAR(20)  NOT NULL DEFAULT '' AFTER customer_name;

-- EXPIRED: sesi pembayaran QRIS habis waktu tanpa dibayar (beda dari FAILED yang eksplisit
-- ditolak gateway).
ALTER TABLE payments
    MODIFY COLUMN status ENUM('PENDING','PAID','FAILED','REFUNDED','EXPIRED') NOT NULL DEFAULT 'PENDING';

-- Kolom sesi pembayaran QRIS dummy — meniru bentuk response Midtrans Core API charge QRIS
-- (transaction_id, qr_string, waktu kedaluwarsa) supaya gateway sungguhan bisa jadi pengganti
-- drop-in di kemudian hari tanpa ubah skema lagi.
ALTER TABLE payments
    ADD COLUMN gateway_transaction_id VARCHAR(64) NULL AFTER status,
    ADD COLUMN qr_payload             TEXT        NULL AFTER gateway_transaction_id,
    ADD COLUMN expires_at             TIMESTAMP   NULL AFTER qr_payload;
