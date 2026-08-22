# AntriGo — Self-Order & Antrean Digital untuk UMKM Kuliner

Aplikasi self-order berbasis QR untuk warung/kedai UMKM. Pelanggan scan QR di meja → pilih menu →
checkout → dapat nomor antrean digital + estimasi waktu tunggu. Admin mengelola menu, stok, papan
antrean dapur, dan laporan penjualan dari satu dashboard.

Proyek ini adalah implementasi nyata (bukan mockup) dari studi kasus final project, sesuai tech stack:

| Layer | Teknologi |
|---|---|
| Backend | Spring Boot 3 (Java 17), Spring Security JWT, Spring Data JPA, Flyway |
| Database | MySQL 8 |
| Cache | In-Memory (Caffeine) |
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

Prasyarat: Docker + Docker Compose terpasang. Tidak perlu install Java/Node/MySQL secara lokal.

```bash
cp backend/.env.example backend/.env      # opsional, default sudah jalan
docker compose up --build
```

Setelah semua container `healthy`:

- Frontend (pelanggan + admin): http://localhost:5173
- Backend API: http://localhost:8080/api/v1
- Swagger / OpenAPI: http://localhost:8080/swagger-ui.html
- MySQL: localhost:3306 (db `antrigo`, user `antrigo`, password `antrigo123`)

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
# butuh MySQL 8 jalan lokal (cache in-memory, tidak perlu server tambahan), sesuaikan src/main/resources/application-local.yml
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
   kategori, pagination, dan cache in-memory.
2. **Checkout dengan concurrency safety** — `POST /api/v1/orders` menjalankan satu transaksi DB:
   baris produk dikunci dengan pessimistic lock (`SELECT ... FOR UPDATE`), **diurutkan menurut
   product id** supaya dua checkout yang overlap tidak saling deadlock. Stok divalidasi ulang di
   dalam kunci; jika kurang → `422` dan seluruh transaksi rollback.
3. **Harga dihitung ulang di server** — payload checkout hanya berisi `productId`, `variantId`,
   `quantity`, `note`. Tidak ada field harga yang diterima dari client.
4. **Nomor antrean unik per hari** — untuk CASH digenerate langsung saat checkout; untuk QRIS baru
   digenerate SETELAH pembayaran dikonfirmasi (lihat poin 11). Dijamin oleh
   `UNIQUE (business_date, queue_number)` di database (nullable-safe — banyak order QRIS yang masih
   menunggu bayar boleh sama-sama `NULL` tanpa bentrok), jadi tidak mungkin ada nomor ganda meski
   race condition tinggi.
5. **Stok sebagai ledger** — setiap perubahan stok (checkout, admin adjustment, pembatalan, **auto-reset
   sesi QRIS kedaluwarsa**) dicatat di `stock_movements`. Kolom `products.stock` hanya cache yang bisa
   direkonsiliasi.
6. **Snapshot harga & nama di order_items** — perubahan menu di kemudian hari tidak mengubah riwayat
   struk pelanggan lama.
7. **RBAC** — endpoint admin dilindungi `@PreAuthorize` (role `ADMIN` / `STAFF`), endpoint pelanggan
   publik.
8. **Estimasi waktu tunggu & polling status** — dihitung dari jumlah pesanan aktif di depan +
   rata-rata waktu proses (dari `store_settings`). Frontend polling status via TanStack Query
   (`refetchInterval`), bukan WebSocket, sesuai keputusan di technical depth.
9. **Papan dapur (kitchen board)** — tampilan Kanban 4 kolom (Antre/Diproses/Siap/Selesai), admin/staff
   drag-and-drop kartu antar kolom (`@dnd-kit/core`) untuk mengubah status. Update dilakukan optimis di
   UI lalu dikonfirmasi ke server; kalau transisi tidak valid (mis. `READY` → `QUEUED`) server menolak
   `409` dan kartu otomatis kembali ke kolom asal.
10. **Laporan otomatis** — produk terlaris, stok rendah, jam tersibuk, tren pendapatan — dihitung dari
    agregasi SQL, di-cache in-memory dengan TTL pendek dan invalidasi saat ada order/stock baru.
11. **Pembayaran QRIS via gateway (dummy Midtrans) + sesi otomatis-reset** — lihat bagian
    [Pembayaran QRIS & Sesi](#pembayaran-qris--sesi-pembayaran) di bawah.
12. **Data pelanggan wajib** — setiap checkout (QRIS maupun CASH) mewajibkan `customerName` dan
    `customerPhone`, divalidasi di `CheckoutRequest` (`@NotBlank`, pola nomor telepon).

## Pembayaran QRIS & Sesi Pembayaran

Checkout dengan `paymentMethod: "QRIS"` **tidak langsung** membuat pesanan yang masuk ke papan dapur:

1. `POST /api/v1/orders` membuat `Order` dengan status **`AWAITING_PAYMENT`** (nomor antrean masih
   kosong) + `Payment` berstatus `PENDING`, lalu memanggil `MidtransGatewayService` untuk "charge" QRIS.
2. Implementasi aktif saat ini, `DummyMidtransGatewayService`, **tidak memanggil api.midtrans.com** —
   ia mensimulasikan bentuk response Midtrans Core API charge QRIS asli: `transaction_id`, `qr_string`
   (di sini disebut `qrPayload`, dipakai frontend untuk render QR code), dan `expiry_time` (default 15
   menit, sama seperti default Midtrans sungguhan). Field-field ini disimpan di `payments`.
3. Response checkout langsung berisi `qrPayload` + `paymentExpiresAt`, dirender jadi QR code di
   frontend (`QrisPaymentModal`, bisa diunduh sebagai JPG).
4. Tombol **"Saya Sudah Bayar"** memanggil `POST /api/v1/orders/{orderNumber}/payments/qris/confirm` —
   ini mensimulasikan callback/webhook yang di produksi sungguhan datang dari Midtrans. Baru di titik
   inilah nomor antrean digenerate dan status pindah ke `QUEUED` (masuk papan dapur).
5. **Sesi otomatis reset kalau tidak dibayar** — `PaymentExpiryScheduler` menyapu tiap 60 detik
   (`app.qris.expiry-sweep-ms`) mencari `Payment` yang `PENDING` dan sudah lewat `expiresAt`: order
   dibatalkan (`CANCELLED`), payment ditandai `EXPIRED`, dan stok yang sempat dikurangi saat checkout
   dikembalikan. Cek yang sama juga jalan "lazy" setiap `GET /api/v1/orders/{orderNumber}` dipanggil,
   jadi tidak perlu menunggu jadwal sweeper untuk pelanggan yang refresh halaman.
6. Frontend menyimpan nomor order QRIS yang masih menunggu bayar di `sessionStorage` (tab-scoped) —
   kalau halaman checkout ter-refresh saat modal QR masih terbuka, sesi otomatis di-resume dari server.

**Untuk pasang Midtrans sungguhan nanti:** buat implementasi baru dari `MidtransGatewayService`
(package `com.antrigo.backend.payment`) yang memanggil Midtrans Core API dengan `MIDTRANS_SERVER_KEY`
sungguhan, tandai `@Primary` — kode pemanggilnya (`OrderCheckoutTransactionalService`) tidak perlu
diubah sama sekali karena bentuk responsnya (`QrisChargeResult`) sudah disamakan dari awal.

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

## Deploy ke platform gratis

`docker-compose.yml` di root cocok untuk dev lokal, tapi platform PaaS gratis (Render, Koyeb,
Vercel, dll.) **tidak menjalankan file docker-compose secara langsung** — tiap service di-deploy
terpisah dan disambungkan lewat environment variable, bukan lewat docker network internal.
Topologinya:

| Komponen | Platform | Catatan |
|---|---|---|
| Frontend (Vite/React) | Vercel (Hobby) | Deploy langsung dari Git, bukan lewat `frontend/Dockerfile` |
| Backend (Spring Boot) | Render / Koyeb / Railway (free instance) | Deploy pakai `backend/Dockerfile` yang sudah ada |
| MySQL | Aiven for MySQL (free tier) | Wajib TLS, auto power-off saat idle lama |

Cache sekarang in-memory (Caffeine, lihat `CacheConfig.java`), jadi tidak ada lagi komponen
Redis terpisah yang perlu di-provision — satu service eksternal lebih sedikit untuk di-setup dan
di-maintain.

Semua env var produksi yang dibutuhkan ada di `backend/.env.example` (bagian "PRODUKSI") dan
`frontend/.env.example`. Ringkasnya:

1. **Aiven for MySQL** — buat service MySQL free tier, salin host/port/user/password. Set
   `DB_SSL_MODE=REQUIRED` (Aiven menolak koneksi tanpa TLS).
2. **Backend di Render/Koyeb/Railway** — deploy dari `backend/Dockerfile`, isi semua env var (lihat
   `backend/.env.example`), biarkan `PORT` di-inject otomatis oleh platform.
3. **Frontend di Vercel** — import repo, set root directory `frontend`, isi
   `VITE_API_BASE_URL=https://<url-backend-anda>/api/v1` (harus `https://`).
4. Setelah frontend punya domain (mis. `https://antrigo.vercel.app`), balik ke platform backend dan
   set `CORS_ORIGINS=https://antrigo.vercel.app` (persis, tanpa trailing slash), lalu redeploy
   backend supaya CORS berlaku.

**Batasan free tier yang perlu disadari:** backend & MySQL gratis biasanya auto-sleep setelah
idle beberapa menit — request pertama setelah bangun bisa lambat (~30–60 detik). Ini cukup untuk
demo/portofolio, tapi bukan untuk beban produksi sungguhan.
