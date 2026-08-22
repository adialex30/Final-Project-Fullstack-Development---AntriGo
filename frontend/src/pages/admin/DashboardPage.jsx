import React from 'react'
import AdminLayout from '../../components/AdminLayout'
import { useDashboard } from '../../api/admin'
import { useAuth } from '../../context/AuthContext'
import { formatIDR } from '../../utils/currency'

function StatCard({ label, value, accent }) {
  return (
    <div className="card p-5">
      <p className="text-xs font-medium uppercase tracking-wide text-ink-400">{label}</p>
      <p className={`mt-1 font-display text-2xl font-semibold ${accent || 'text-ink-800'}`}>{value}</p>
    </div>
  )
}

export default function DashboardPage() {
    const { data, isLoading } = useDashboard()
    const { user } = useAuth()
    const isAdmin = user?.role === 'ADMIN'

    return (
        <AdminLayout>
            <h1 className="font-display text-2xl font-semibold text-ink-800">Dashboard</h1>
            <p className="text-sm text-ink-500">Ringkasan operasional hari ini</p>

            {isLoading || !data ? (
                <p className="mt-6 text-ink-400">Memuat laporan...</p>
            ) : (
                <>
                    <div className={`mt-6 grid grid-cols-1 gap-4 ${isAdmin ? 'sm:grid-cols-3' : 'sm:grid-cols-2'}`}>
                        {isAdmin && (
                            <StatCard label="Pendapatan Hari Ini" value={formatIDR(data.revenueToday)} accent="text-ember-600" />
                        )}
                        <StatCard label="Pesanan Hari Ini" value={data.ordersToday} />
                        <StatCard label="Sedang Aktif" value={data.ordersActiveNow} accent="text-sprout-600" />
                    </div>

          <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div className="card p-5">
              <h2 className="font-display text-lg font-semibold text-ink-800">Produk Terlaris</h2>
              <div className="mt-3 space-y-2">
                {data.topProducts.length === 0 && <p className="text-sm text-ink-400">Belum ada data.</p>}
                {data.topProducts.map((p) => (
                  <div key={p.productId} className="flex items-center justify-between text-sm">
                    <span className="text-ink-700">{p.productName}</span>
                    <span className="font-semibold text-ink-800">{p.totalQty} terjual</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="card p-5">
              <h2 className="font-display text-lg font-semibold text-ink-800">Stok Rendah</h2>
              <div className="mt-3 space-y-2">
                {data.lowStockProducts.length === 0 && (
                  <p className="text-sm text-ink-400">Semua stok aman.</p>
                )}
                {data.lowStockProducts.map((p) => (
                  <div key={p.productId} className="flex items-center justify-between text-sm">
                    <span className="text-ink-700">{p.productName}</span>
                    <span className="font-semibold text-chili-600">{p.stock} tersisa</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="card mt-6 p-5">
            <h2 className="font-display text-lg font-semibold text-ink-800">Jam Tersibuk</h2>
            <div className="mt-4 flex items-end gap-2" style={{ height: 120 }}>
              {data.busiestHours.length === 0 && <p className="text-sm text-ink-400">Belum ada data.</p>}
              {data.busiestHours.map((h) => {
                const max = Math.max(...data.busiestHours.map((x) => x.orderCount), 1)
                return (
                  <div key={h.hour} className="flex flex-1 flex-col items-center gap-1">
                    <div
                      className="w-full rounded-t-md bg-ember-400"
                      style={{ height: `${(h.orderCount / max) * 90 + 10}px` }}
                    />
                    <span className="text-[10px] text-ink-400">{h.hour}:00</span>
                  </div>
                )
              })}
            </div>
          </div>
        </>
      )}
    </AdminLayout>
  )
}
