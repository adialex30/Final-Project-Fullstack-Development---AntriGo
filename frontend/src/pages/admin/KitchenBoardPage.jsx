import React from 'react'
import toast from 'react-hot-toast'
import AdminLayout from '../../components/AdminLayout'
import StatusBadge from '../../components/StatusBadge'
import { useKitchenBoard, useUpdateOrderStatus, useConfirmCashPayment } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

const NEXT_STATUS = {
  QUEUED: 'PROCESSING',
  PROCESSING: 'READY',
  READY: 'COMPLETED',
}
const NEXT_LABEL = {
  QUEUED: 'Mulai Proses',
  PROCESSING: 'Tandai Siap',
  READY: 'Selesaikan',
}

export default function KitchenBoardPage() {
  const { data: board, isLoading } = useKitchenBoard()
  const updateStatus = useUpdateOrderStatus()
  const confirmCash = useConfirmCashPayment()

  const handleAdvance = async (order) => {
    const next = NEXT_STATUS[order.status]
    if (!next) return
    try {
      await updateStatus.mutateAsync({ orderNumber: order.orderNumber, status: next })
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal mengubah status'))
    }
  }

  const handleCancel = async (order) => {
    try {
      await updateStatus.mutateAsync({ orderNumber: order.orderNumber, status: 'CANCELLED', note: 'Dibatalkan dari papan dapur' })
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal membatalkan'))
    }
  }

  return (
    <AdminLayout>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink-800">Papan Dapur</h1>
          <p className="text-sm text-ink-500">Update otomatis setiap 5 detik</p>
        </div>
      </div>

      {isLoading ? (
        <p className="mt-6 text-ink-400">Memuat papan antrean...</p>
      ) : board.length === 0 ? (
        <p className="mt-10 text-center text-ink-400">Belum ada pesanan aktif.</p>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {board.map((order) => (
            <div key={order.orderNumber} className="card p-4">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-display text-3xl font-bold text-ink-800">
                    {String(order.queueNumber).padStart(2, '0')}
                  </p>
                  {order.tableNumber && <p className="text-xs text-ink-400">Meja {order.tableNumber}</p>}
                </div>
                <StatusBadge status={order.status} />
              </div>

              <div className="mt-3 space-y-1 border-t border-ink-100 pt-3">
                {order.items.map((item, idx) => (
                  <p key={idx} className="text-sm text-ink-700">
                    {item.quantity}× {item.productName}
                    {item.variantName ? ` (${item.variantName})` : ''}
                  </p>
                ))}
              </div>

              <p className="mt-2 text-xs text-ink-400">Menunggu {order.waitingMinutes} menit</p>

              <div className="mt-4 flex gap-2">
                {NEXT_STATUS[order.status] && (
                  <button
                    onClick={() => handleAdvance(order)}
                    disabled={updateStatus.isPending}
                    className="btn-primary flex-1 py-1.5 text-sm"
                  >
                    {NEXT_LABEL[order.status]}
                  </button>
                )}
                {order.status === 'QUEUED' && (
                  <button
                    onClick={() => handleCancel(order)}
                    className="btn-danger px-3 py-1.5 text-sm"
                  >
                    Batal
                  </button>
                )}
              </div>

              <button
                onClick={() => confirmCash.mutate(order.orderNumber)}
                className="mt-2 w-full text-xs font-medium text-ink-500 underline decoration-dotted"
              >
                Konfirmasi bayar tunai
              </button>
            </div>
          ))}
        </div>
      )}
    </AdminLayout>
  )
}
