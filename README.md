# AntriGo — Self-Order & Antrean Digital untuk UMKM Kuliner

Aplikasi self-order berbasis QR untuk warung/kedai UMKM. Pelanggan scan QR di meja → pilih menu →
checkout → dapat nomor antrean digital + estimasi waktu tunggu. Admin mengelola menu, stok, papan
antrean dapur, dan laporan penjualan dari satu dashboard.

Proyek ini adalah implementasi nyata (bukan mockup) dari studi kasus final project, sesuai tech stack:

| Layer | Teknologi |
|---|---|
| Backend | Spring Boot 3 (Java 17), Spring Security JWT, Spring Data JPA, Flyway |
| Database | MySQL 8 |
| Cache | Redis |
| Frontend | React 19 + Vite, Tailwind CSS, TanStack Query, React Router, Axios |
| Infra | Docker Compose |
| API Testing | Postman + Newman (automation testing) |

## Struktur folder

```
antrigo/
├── backend/          # Spring Boot API
├── frontend/         # React + Vite SPA
├── postman/          # Collection, environment, Newman automation
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Menjalankan seluruh stack (Docker Compose)

Prasyarat: Docker + Docker Compose terpasang. Tidak perlu install Java/Node/MySQL/Redis secara lokal.

```bash
cp backend/.env.example backend/.env      # opsional, default sudah jalan
docker compose up --build
```

Setelah semua container `healthy`:

- Frontend (pelanggan + admin): http://localhost:5173
- Backend API: http://localhost:8080/api/v1
- Swagger / OpenAPI: http://localhost:8080/swagger-ui.html
- MySQL: localhost:3306 (db `antrigo`, user `antrigo`, password `antrigo123`)
- Redis: localhost:6379

Flyway otomatis menjalankan migration + seed data saat backend start pertama kali (lihat
`backend/src/main/resources/db/migration`). Seed berisi 1 akun admin, beberapa kategori, produk,
dan varian supaya dashboard tidak terlihat kosong.

**Akun admin default (seed):**
- email: `admin@antrigo.id`
- password: `Admin12345`

## Menjalankan tanpa Docker (development)

**Backend**
```bash
cd backend
# butuh MySQL 8 + Redis jalan lokal, sesuaikan src/main/resources/application-local.yml
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

## Alur bisnis inti yang diimplementasikan

1. **Katalog & keranjang** — publik, tanpa login. `GET /api/v1/products` dengan search, filter
   kategori, pagination, dan cache Redis.
2. **Checkout dengan concurrency safety** — `POST /api/v1/orders` menjalankan satu transaksi DB:
   baris produk dikunci dengan pessimistic lock (`SELECT ... FOR UPDATE`), **diurutkan menurut
   product id** supaya dua checkout yang overlap tidak saling deadlock. Stok divalidasi ulang di
   dalam kunci; jika kurang → `422` dan seluruh transaksi rollback.
3. **Harga dihitung ulang di server** — payload checkout hanya berisi `productId`, `variantId`,
   `quantity`, `note`. Tidak ada field harga yang diterima dari client.
4. **Nomor antrean unik per hari** — digenerate di transaksi yang sama dengan checkout, dijamin oleh
   `UNIQUE (business_date, queue_number)` di database, jadi tidak mungkin ada nomor ganda meski
   race condition tinggi.
5. **Stok sebagai ledger** — setiap perubahan stok (checkout, admin adjustment, pembatalan) dicatat
   di `stock_movements`. Kolom `products.stock` hanya cache yang bisa direkonsiliasi.
6. **Snapshot harga & nama di order_items** — perubahan menu di kemudian hari tidak mengubah riwayat
   struk pelanggan lama.
7. **RBAC** — endpoint admin dilindungi `@PreAuthorize` (role `ADMIN` / `STAFF`), endpoint pelanggan
   publik.
8. **Estimasi waktu tunggu & polling status** — dihitung dari jumlah pesanan aktif di depan +
   rata-rata waktu proses (dari `store_settings`). Frontend polling status via TanStack Query
   (`refetchInterval`), bukan WebSocket, sesuai keputusan di technical depth.
9. **Papan dapur (kitchen board)** — admin/staff mengubah status pesanan satu klik, transisi status
   divalidasi (`409` jika transisi tidak valid, mis. `READY` → `QUEUED`).
10. **Laporan otomatis** — produk terlaris, stok rendah, jam tersibuk, tren pendapatan — dihitung dari
    agregasi SQL, di-cache di Redis dengan TTL pendek dan invalidasi saat ada order/stock baru.

## Data Seeder (demo data)

Selain katalog dasar dari Flyway (`V2__seed_data.sql`), ada `DemoDataSeeder` — sebuah
`CommandLineRunner` Spring Boot yang membangun **~10 hari riwayat transaksi realistis**
(pesanan, item, pembayaran, audit trail status, dan pergerakan stok) di atas katalog yang sudah
ada, supaya dashboard, laporan, dan papan dapur langsung terisi data begitu aplikasi pertama
kali dijalankan — pas untuk demo/presentasi tanpa perlu klik-klik manual dulu.

**Lokasi:** `backend/src/main/java/com/antrigo/backend/seeder/DemoDataSeeder.java`

**Yang di-generate:**
- Pesanan tersebar di 10 hari terakhir, jam dibobotkan ke jam makan siang (11–14) dan makan
  malam (17–20) — supaya grafik "Jam Tersibuk" di dashboard terlihat masuk akal, bukan rata.
- Hari-hari lampau mayoritas `COMPLETED` dengan sedikit `CANCELLED` (~4–6%); hari ini sengaja
  dicampur `QUEUED` / `PROCESSING` / `READY` / `COMPLETED` supaya papan dapur terlihat "sedang
  berjalan" saat demo, bukan semuanya sudah selesai.
- Setiap pesanan tetap melewati aturan bisnis yang sama seperti checkout sungguhan: harga
  snapshot dari harga produk saat itu, stok dilacak dan tidak pernah dibiarkan minus, transisi
  status dicatat di `order_status_logs` (dengan aktor staff untuk transisi setelah `QUEUED`),
  dan pembatalan mencatat `stock_movements` tipe `CANCELLATION_REVERSAL`.
- Random pakai seed tetap (`new Random(42)`) — hasilnya reproducible, bukan berubah-ubah tiap
  kali di-generate ulang dari database kosong.

**Yang TIDAK dilakukan seeder ini:** membuat kategori atau produk baru. Ia murni membangun
riwayat transaksi di atas katalog yang sudah ada dari Flyway — supaya tidak tumpang tindih
tanggung jawab dengan migration.

### Mengaktifkan / menonaktifkan

Nonaktif secara default (`app.seed.demo-data=false`). **Di `docker-compose.yml` sudah
diaktifkan secara default** (`SEED_DEMO_DATA=true`) supaya `docker compose up` langsung
memberi dashboard yang terisi.

```bash
# Docker: matikan seeding demo data kalau tidak diinginkan
SEED_DEMO_DATA=false docker compose up --build

# Jalan lokal tanpa Docker, aktifkan sekali:
mvn spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--app.seed.demo-data=true
```

**Idempoten** — seeder mengecek dulu apakah tabel `orders` sudah berisi data; kalau sudah, ia
langsung berhenti tanpa melakukan apa-apa. Aman dibiarkan `true` permanen di `docker-compose.yml`
karena hanya benar-benar mengisi data pada boot pertama saat database masih kosong.

**Reset data demo** (misalnya mau data "hari ini" yang segar persis di hari presentasi):

```bash
docker compose down -v   # hapus volume MySQL — database benar-benar kosong lagi
docker compose up --build
```



Lihat `postman/README.md`. Ringkasnya:

```bash
cd postman
npm install
npm run test:local     # jalankan collection ke http://localhost:8080
```

Collection mencakup: auth, CRUD produk (admin), alur checkout pelanggan penuh (create → pay →
polling status), papan dapur admin, validasi 403/409/422, dan skenario stok habis.
