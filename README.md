# AntriGo — Self-Order & Antrean Digital untuk UMKM Kuliner

Aplikasi self-order berbasis QR untuk warung/kedai UMKM. Pelanggan scan QR di meja → pilih menu →
checkout → bayar QRIS (Midtrans Sandbox) → dapat nomor antrean digital + estimasi waktu tunggu.
Admin & staff mengelola menu, stok, papan antrean dapur, dan laporan penjualan dari satu dashboard
dengan hak akses berbeda per role.

**Live demo:**
- Frontend (pelanggan + admin): https://antrigo-aditya-dwi.vercel.app
- Backend API: https://antrigo-be.up.railway.app/api/v1
- Swagger / OpenAPI: https://antrigo-be.up.railway.app/swagger-ui.html

## Tech Stack

| Layer | Teknologi |
|---|---|
| Backend | Spring Boot 3 (Java 17), Spring Security (JWT, stateless), Spring Data JPA, Flyway |
| Database | MySQL 8 |
| Cache | In-memory (Caffeine) — `products`, `categories`, `reports` |
| Payment Gateway | Midtrans Core API (QRIS, Sandbox) |
| Frontend | React 19 + Vite, Tailwind CSS, TanStack Query, React Router, Axios |
| Deploy | Backend: Railway (Docker) · Frontend: Vercel |
| API Docs | springdoc-openapi (Swagger UI) |
| API Testing | Postman collection |

## Struktur Folder

```
antrigo/
├── backend/          # Spring Boot API
├── frontend/         # React + Vite SPA
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Role & Hak Akses

| Halaman | Admin | Staff |
|---|:---:|:---:|
| Dashboard | ✅ (termasuk Pendapatan Hari Ini) | ✅ (tanpa angka Pendapatan) |
| Papan Dapur | — | ✅ |
| Produk & Stok | ✅ | — |
| Laporan | ✅ | — |
| Registrasi Staff | ✅ | — |

Dikunci di **dua lapis**: route/menu disembunyikan di frontend (`ProtectedRoute` + filter nav), dan
endpoint terkait di backend dilindungi `@PreAuthorize` sesuai role — jadi tidak bisa dilewati lewat
pemanggilan API langsung meski link UI-nya disembunyikan.

Registrasi akun staff/admin baru hanya bisa dilakukan oleh admin yang sudah login
(`POST /api/v1/auth/register`, `@PreAuthorize("hasRole('ADMIN')")`) — tidak ada pendaftaran publik.

## Menjalankan Lokal (Docker Compose)

Prasyarat: Docker + Docker Compose. Tidak perlu install Java/Node/MySQL manual.

```bash
cp backend/.env.example backend/.env      # opsional, default sudah jalan
docker compose up --build
```

Setelah semua container `healthy`:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html
- MySQL: localhost:3306 (db `antrigo`, user `antrigo`, password `antrigo123`)

**Akun admin default (seed):** `admin@antrigo.id` / `Admin12345`

## Menjalankan Tanpa Docker

**Backend**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

## Alur Bisnis Inti

1. **Katalog publik** — `GET /api/v1/products` (search, filter kategori, pagination), `GET /api/v1/categories` — tanpa login, di-cache in-memory (Caffeine).
2. **Checkout dengan concurrency safety** — `POST /api/v1/orders` mengunci baris produk (`SELECT ... FOR UPDATE`, diurutkan per `productId` supaya tidak deadlock), memvalidasi ulang stok di dalam kunci, dan menghitung ulang harga di server (client hanya kirim `productId`/`variantId`/`quantity`).
3. **Nomor antrean unik per hari** — CASH langsung digenerate saat checkout; QRIS digenerate setelah pembayaran dikonfirmasi. Dijamin `UNIQUE (business_date, queue_number)`.
4. **Pembayaran QRIS via Midtrans (Sandbox)** — lihat bagian [Pembayaran QRIS](#pembayaran-qris--sesi-pembayaran).
5. **Sesi QRIS auto-expire** — `PaymentExpiryScheduler` menyapu tiap 60 detik, membatalkan order & mengembalikan stok kalau sesi QRIS tidak dibayar dalam `QRIS_EXPIRY_MINUTES` (default 15 menit). Timestamp expiry dikirim ke frontend sebagai `Instant` (UTC, ISO-8601 dengan `Z`) supaya perhitungan countdown di browser akurat terlepas dari timezone perangkat pengguna.
6. **Stok sebagai ledger** — setiap perubahan (checkout, adjustment admin, pembatalan, auto-reset QRIS) dicatat di `stock_movements`.
7. **Papan dapur (Kitchen Board)** — Kanban 4 kolom (Antre/Diproses/Siap/Selesai), staff mengubah status pesanan.
8. **Laporan otomatis** — produk terlaris, stok rendah, jam tersibuk, pendapatan harian — agregasi SQL, di-cache in-memory.
9. **RBAC** — lihat tabel [Role & Hak Akses](#role--hak-akses) di atas.

## Pembayaran QRIS & Sesi Pembayaran

1. Checkout dengan `paymentMethod: "QRIS"` membuat `Order` berstatus `AWAITING_PAYMENT` + `Payment` berstatus `PENDING`, lalu memanggil Midtrans Core API (`/v2/qris/charge`, Sandbox) lewat `RealMidtransGatewayService`.
2. Response Midtrans (`transaction_id`, QR image URL, `expiry_time`) disimpan; frontend merender QR (`QrisPaymentModal`) dengan countdown & progress bar, bisa diunduh sebagai PNG.
3. Setelah pelanggan scan & bayar, Midtrans mengirim webhook ke `POST /api/v1/payments/midtrans/notification` — di titik ini nomor antrean digenerate dan status pindah ke `QUEUED`.
4. Kalau tidak dibayar dalam batas waktu, `PaymentExpiryScheduler` membatalkan order otomatis dan mengembalikan stok.

**Environment variable yang wajib diisi di Railway** (backend):
```
MIDTRANS_MERCHANT_ID=<dari Midtrans Dashboard, mode Sandbox>
MIDTRANS_CLIENT_KEY=SB-Mid-client-...
MIDTRANS_SERVER_KEY=SB-Mid-server-...
MIDTRANS_SANDBOX=true
```
Payment Notification URL di Midtrans Dashboard diarahkan ke:
```
https://antrigo-be.up.railway.app/api/v1/payments/midtrans/notification
```

## API Documentation

Swagger UI otomatis dari `springdoc-openapi`, live di:
```
https://antrigo-be.up.railway.app/swagger-ui.html
```
Spec mentah (OpenAPI 3 JSON): `https://antrigo-be.up.railway.app/v3/api-docs`

## Postman Collection

Import `AntriGo.postman_collection.json` ke Postman. Variable koleksi:
- `baseUrl` — sudah diarahkan ke production Railway, ganti ke `http://localhost:8080/api/v1` untuk testing lokal
- `token` — otomatis terisi setelah request **Auth → Login** berhasil (lewat test script)
- `orderNumber`, `productId`, `categoryId` — ganti sesuai data aktual

Struktur folder collection: Auth · Public (Menu, Orders & Payment) · Admin/Staff (Kitchen & Orders) ·
Admin (Products & Categories, Stock, Reports) — mengikuti pembagian akses role yang sama seperti tabel
di atas.

## Deploy

| Komponen | Platform | Catatan |
|---|---|---|
| Frontend (Vite/React) | Vercel | `VITE_API_BASE_URL` wajib `https://...` lengkap + redeploy setelah env var diubah (Vite bake saat build) |
| Backend (Spring Boot) | Railway | Deploy dari `backend/Dockerfile`; `CORS_ORIGINS` harus persis domain Vercel |
| MySQL | Railway MySQL plugin | — |

Env var backend penting: `DB_HOST/PORT/NAME/USER/PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`,
`MIDTRANS_*` (lihat di atas), `QRIS_EXPIRY_MINUTES`, `SPRING_PROFILES_ACTIVE=docker`.

**Batasan free tier:** Railway free instance bisa auto-sleep saat idle lama — request pertama setelah
bangun bisa lambat.
