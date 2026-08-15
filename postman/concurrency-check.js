// concurrency-check.js
// Membuktikan pessimistic locking di OrderCheckoutTransactionalService benar-benar mencegah
// oversell saat banyak pelanggan checkout produk yang sama secara bersamaan.
//
// Persiapan:
//   1. Set stok produk target ke angka kecil (mis. 1) lewat dashboard admin atau:
//      POST /api/v1/admin/stock/{productId}/adjust  { "quantityChange": <selisih ke 1>, "note": "k6 setup" }
//   2. Jalankan:
//      k6 run --vus 50 --iterations 50 -e BASE_URL=http://localhost:8080/api/v1 \
//              -e PRODUCT_ID=<id_produk_stok_1> concurrency-check.js
//
// Ekspektasi hasil: tepat 1 request sukses (201), sisanya 422 (stok tidak mencukupi),
// dan stok akhir produk = 0 (tidak pernah minus, tidak ada nomor antrean ganda).

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const success201 = new Counter('checkout_success_201');
const rejected422 = new Counter('checkout_rejected_422');
const unexpected = new Counter('checkout_unexpected_status');

export const options = {
  vus: 50,
  iterations: 50,
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
  const productId = __ENV.PRODUCT_ID;

  if (!productId) {
    throw new Error('Set -e PRODUCT_ID=<id produk dengan stok kecil> sebelum menjalankan script ini');
  }

  const payload = JSON.stringify({
    tableNumber: `k6-${__VU}`,
    paymentMethod: 'CASH',
    items: [{ productId: Number(productId), quantity: 1 }],
  });

  const res = http.post(`${baseUrl}/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 201 or 422': (r) => r.status === 201 || r.status === 422,
  });

  if (res.status === 201) success201.add(1);
  else if (res.status === 422) rejected422.add(1);
  else unexpected.add(1);
}

export function handleSummary(data) {
  console.log('--- Ringkasan concurrency test ---');
  console.log('Checkout sukses (201):', data.metrics.checkout_success_201?.values.count || 0);
  console.log('Checkout ditolak stok (422):', data.metrics.checkout_rejected_422?.values.count || 0);
  console.log('Status tak terduga:', data.metrics.checkout_unexpected_status?.values.count || 0);
  console.log('Verifikasi manual setelahnya: GET /products/{id} -> stock harus 0, bukan negatif.');
  return {};
}
