import React from 'react'
import { useParams, Link } from 'react-router-dom'
import CustomerNavbar from '../../components/CustomerNavbar'
import StatusBadge from '../../components/StatusBadge'
import { useOrder, useOrderStatus, useCancelOrder } from '../../api/orders'
import { extractErrorMessage } from '../../api/client'
import { formatIDR } from '../../utils/currency'
import toast from 'react-hot-toast'

const STEPS = ['QUEUED', 'PROCESSING', 'READY', 'COMPLETED']

export default function QueueStatusPage() {
  const { orderNumber } = useParams()
  const { data: order } = useOrder(orderNumber)
  const { data: status } = useOrderStatus(orderNumber)
  const cancelOrder = useCancelOrder()

  const currentStatus = status?.status || order?.status
  const stepIndex = STEPS.indexOf(currentStatus)
  const isCancelled = currentStatus === 'CANCELLED'

  const handleCancel = async () => {
    try {
      await cancelOrder.mutateAsync(orderNumber)
      toast.success('Pesanan dibatalkan')
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal membatalkan pesanan'))
    }
  }

  if (!order) {
    return (
      <div className="min-h-screen">
        <CustomerNavbar />
        <p className="mt-10 text-center text-ink-400">Memuat pesanan...</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen">
      <CustomerNavbar />
      <div className="mx-auto max-w-3xl px-4 py-6">
        <div className="card overflow-hidden">
          <div className="bg-ink-800 px-6 py-8 text-center text-paper">
            <p className="text-xs uppercase tracking-[0.2em] text-ember-300">Nomor Antrean Anda</p>
            <p className="font-display text-7xl font-bold leading-none text-white">
              {String(order.queueNumber).padStart(2, '0')}
            </p>
            <p className="mt-2 text-sm text-ink-300">{order.orderNumber}</p>
          </div>

          <div className="p-6">
            <div className="flex items-center justify-between">
              <StatusBadge status={currentStatus} />
              {!isCancelled && currentStatus !== 'COMPLETED' && (
                <p className="text-sm text-ink-500">
                  Estimasi tunggu: <span className="font-semibold text-ink-800">{status?.estimatedWaitMinutes ?? order.estimatedWaitMinutes} menit</span>
                </p>
              )}
            </div>

            {!isCancelled && (
              <div className="mt-5 flex items-center justify-between">
                {STEPS.map((step, idx) => (
                  <React.Fragment key={step}>
                    <div className="flex flex-col items-center gap-1">
                      <div
                        className={`h-3 w-3 rounded-full ${
                          idx <= stepIndex ? 'bg-ember-500' : 'bg-ink-200'
                        }`}
                      />
                      <span className={`text-[10px] ${idx <= stepIndex ? 'text-ink-700' : 'text-ink-300'}`}>
                        {step === 'QUEUED' && 'Antre'}
                        {step === 'PROCESSING' && 'Diproses'}
                        {step === 'READY' && 'Siap'}
                        {step === 'COMPLETED' && 'Selesai'}
                      </span>
                    </div>
                    {idx < STEPS.length - 1 && (
                      <div className={`h-0.5 flex-1 ${idx < stepIndex ? 'bg-ember-500' : 'bg-ink-200'}`} />
                    )}
                  </React.Fragment>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="card mt-4 p-5">
          <h2 className="font-display text-lg font-semibold text-ink-800">Struk Digital</h2>
          <div className="mt-3 space-y-2">
            {order.items.map((item, idx) => (
              <div key={idx} className="flex justify-between text-sm">
                <span className="text-ink-600">
                  {item.quantity}× {item.productName}
                  {item.variantName ? ` (${item.variantName})` : ''}
                </span>
                <span className="font-medium text-ink-800">{formatIDR(item.lineTotal)}</span>
              </div>
            ))}
          </div>
          <div className="mt-3 flex justify-between border-t border-ink-100 pt-3 font-semibold text-ink-800">
            <span>Total</span>
            <span>{formatIDR(order.totalAmount)}</span>
          </div>
          {order.tableNumber && (
            <p className="mt-2 text-xs text-ink-400">Meja {order.tableNumber}</p>
          )}
          {order.customerName && (
            <p className="mt-1 text-xs text-ink-400">Atas nama {order.customerName}</p>
          )}
        </div>

        {currentStatus === 'QUEUED' && (
          <button
            onClick={handleCancel}
            disabled={cancelOrder.isPending}
            className="btn-danger mt-4 w-full"
          >
            Batalkan Pesanan
          </button>
        )}

        <Link to="/" className="btn-secondary mt-3 flex w-full">
          Pesan Lagi
        </Link>
      </div>
    </div>
  )
}
