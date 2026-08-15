# AntriGo — API Automation Testing (Postman / Newman)

`AntriGo.postman_collection.json` berisi **43 request** terbagi 8 folder, dijalankan berurutan
(request saling chaining lewat environment variable — bukan sekadar kumpulan request lepas).

| Folder | Isi |
|---|---|
| 0. Setup | Health check, login admin, resolve id produk/kategori dari seed data (dinamis, bukan hardcode) |
| 1. Public Catalog | Katalog & pencarian — tanpa login |
| 2. Admin - Product & Category Management | CRUD produk/kategori + RBAC (401 tanpa token, 403 role salah) |
| 3. Admin - Stock Management | Restock, percobaan bikin stok negatif (422), laporan stok rendah |
| 4. Customer - Checkout Flow (Happy Path) | Checkout → cek harga dihitung ulang server → polling status → verifikasi stok berkurang |
| 5. Customer - Negative & Edge Cases | Stok tidak cukup (422), produk tidak ada (404), keranjang kosong (400), pembatalan + verifikasi stok balik, cancel ganda (409) |
| 6. Admin - Kitchen Board & Status Transitions | Papan dapur, urutan antrean, state machine status (409 untuk transisi mundur) |
| 7. Admin - Reports | Dashboard & validasi struktur laporan |

## Menjalankan

**Via Postman app:**
1. Import `AntriGo.postman_collection.json` dan `AntriGo.postman_environment.json`.
2. Pilih environment **AntriGo Local**, pastikan `base_url` mengarah ke backend yang jalan
   (default `http://localhost:8080/api/v1`).
3. Klik **Run collection**, jalankan seluruh folder secara berurutan (0 → 7).

**Via Newman (CLI/CI):**
```bash
npm install
npm run test:local     # laporan HTML di newman/report.html
npm run test:ci        # laporan JUnit XML (dipakai di GitHub Actions)
```

Prasyarat: backend + MySQL + Redis sudah jalan (`docker compose up` di root project) dan
sudah melalui Flyway seed data (admin default `admin@antrigo.id` / `Admin12345`).

## Aman dijalankan di stack yang sudah diisi DemoDataSeeder

Sejak `DemoDataSeeder` ada (lihat `backend/.../seeder/DemoDataSeeder.java`), `docker compose up`
secara default langsung mengisi ~10 hari riwayat transaksi supaya dashboard tidak kosong saat
demo. Assertion stok di folder 4 & 5 collection ini **tidak** memakai angka absolut dari seed
awal — ada request "Snapshot Nasi Goreng Stock (Before Checkout)" yang mengambil baseline stok
tepat sebelum checkout, lalu assertion berikutnya membandingkan terhadap baseline itu, bukan
angka hardcode. Jadi collection ini valid dijalankan baik di database yang masih bersih (baru
Flyway, belum ada demo data) maupun yang sudah diisi DemoDataSeeder.

## Kenapa didesain begini

- **Id produk di-resolve secara dinamis** (folder 0) dengan mencari berdasarkan nama produk seed,
  bukan hardcode id — supaya collection tetap valid meski urutan seed berubah.
- **Setiap folder membangun state untuk folder berikutnya** (order_number, board_order_number,
  dst.) — mensimulasikan alur nyata, bukan test terisolasi yang tidak merepresentasikan
  penggunaan sebenarnya.
- **Assertion mencakup jalur negatif secara eksplisit**: 401 (tanpa token), 403 (role salah),
  404 (resource tidak ada), 409 (transisi status tidak valid / order sudah dibatalkan),
  422 (stok tidak mencukupi), 400 (validasi payload) — sesuai kontrak HTTP status di technical
  depth pada proposal.
- **Verifikasi efek samping, bukan cuma status code** — setelah checkout, folder berikutnya
  memanggil `GET /products/{id}` untuk membuktikan stok benar-benar berkurang; setelah cancel,
  memverifikasi stok benar-benar dikembalikan. Ini yang membedakan automation testing dari
  sekadar smoke test.

## Tentang uji concurrency (race condition stok)

Newman menjalankan request **secara sekuensial**, jadi tidak representatif untuk membuktikan
pessimistic locking di `OrderCheckoutTransactionalService`. Untuk itu, uji concurrency
sebaiknya pakai tool yang mendukung request paralel sungguhan, misalnya k6:

```js
// concurrency-check.js — jalankan: k6 run --vus 50 --iterations 50 concurrency-check.js
import http from 'k6/http';

export default function () {
  const payload = JSON.stringify({
    paymentMethod: 'CASH',
    items: [{ productId: __ENV.LOW_STOCK_PRODUCT_ID, quantity: 1 }],
  });
  http.post(`${__ENV.BASE_URL}/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
}
```

Skenario: set stok produk ke 1, tembak 50 checkout paralel memesan produk yang sama —
harapannya tepat 1 sukses (201), 49 gagal 422, dan stok akhir tepat 0 (bukan minus).
Ini skenario yang disebut di slide "Issue & Problem Solving" sebagai bukti konkret, bukan
sekadar klaim desain.
