import React from 'react'
import AdminLayout from '../../components/AdminLayout'
import { useDashboard, useLowStock } from '../../api/admin'
import { formatIDR } from '../../utils/currency'

export default function ReportsPage() {
  const { data: dashboard, isLoading } = useDashboard()
  const { data: lowStock } = useLowStock()

  return (
    <AdminLayout>
      <h1 className="font-display text-2xl font-semibold text-ink-800">Laporan</h1>
      <p className="text-sm text-ink-500">Data untuk keputusan menu & belanja bahan — bukan perkiraan</p>

      {isLoading || !dashboard ? (
        <p className="mt-6 text-ink-400">Memuat laporan...</p>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div className="card p-5">
            <h2 className="font-display text-lg font-semibold text-ink-800">Produk Terlaris (Semua Waktu)</h2>
            <table className="mt-3 w-full text-sm">
              <thead className="text-left text-xs uppercase text-ink-400">
                <tr>
                  <th className="py-1.5">Produk</th>
                  <th className="py-1.5 text-right">Terjual</th>
                  <th className="py-1.5 text-right">Pendapatan</th>
                </tr>
              </thead>
              <tbody>
                {dashboard.topProducts.map((p) => (
                  <tr key={p.productId} className="border-t border-ink-100">
                    <td className="py-2 text-ink-700">{p.productName}</td>
                    <td className="py-2 text-right font-medium">{p.totalQty}</td>
                    <td className="py-2 text-right font-medium text-ember-600">{formatIDR(p.totalRevenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="card p-5">
            <h2 className="font-display text-lg font-semibold text-ink-800">Stok Rendah</h2>
            <p className="text-xs text-ink-400">Segera lakukan restock supaya tidak kehabisan saat jam ramai</p>
            <div className="mt-3 space-y-2">
              {(lowStock || []).length === 0 && <p className="text-sm text-ink-400">Semua stok dalam batas aman.</p>}
              {(lowStock || []).map((p) => (
                <div key={p.productId} className="flex items-center justify-between rounded-lg bg-chili-500/5 px-3 py-2 text-sm">
                  <span className="text-ink-700">{p.productName}</span>
                  <span className="font-semibold text-chili-600">
                    {p.stock} / batas {p.threshold}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  )
}
