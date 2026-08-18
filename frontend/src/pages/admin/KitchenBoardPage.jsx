import React, { useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { DndContext, DragOverlay, PointerSensor, TouchSensor, useDraggable, useDroppable, useSensor, useSensors } from '@dnd-kit/core'
import AdminLayout from '../../components/AdminLayout'
import StatusBadge from '../../components/StatusBadge'
import { useKitchenBoard, useUpdateOrderStatus, useConfirmCashPayment } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

const COLUMNS = [
  { status: 'QUEUED', label: 'Antre' },
  { status: 'PROCESSING', label: 'Diproses' },
  { status: 'READY', label: 'Siap' },
  { status: 'COMPLETED', label: 'Selesai' },
]

function OrderCard({ order, onCancel, onConfirmCash, dragging }) {
  const { attributes, listeners, setNodeRef, transform } = useDraggable({
    id: order.orderNumber,
    data: { status: order.status },
    disabled: order.status === 'COMPLETED',
  })

  const style = transform ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` } : undefined

  return (
      <div ref={setNodeRef} style={style} className={`card p-3 ${dragging ? 'opacity-40' : ''} ${order.status !== 'COMPLETED' ? 'touch-none' : ''}`}>
        <div {...listeners} {...attributes} className={order.status !== 'COMPLETED' ? 'cursor-grab active:cursor-grabbing' : ''}>
          <div className="flex items-start justify-between">
            <div>
              <p className="font-display text-2xl font-bold text-ink-800">{String(order.queueNumber ?? '-').padStart(2, '0')}</p>
              {order.customerName && <p className="text-xs font-medium text-ink-600">{order.customerName}</p>}
              {order.tableNumber && <p className="text-xs text-ink-400">Meja {order.tableNumber}</p>}
            </div>
            <StatusBadge status={order.status} />
          </div>

          <div className="mt-2 space-y-1 border-t border-ink-100 pt-2">
            {order.items.map((item, idx) => (
                <p key={idx} className="text-sm text-ink-700">
                  {item.quantity}× {item.productName}{item.variantName ? ` (${item.variantName})` : ''}
                </p>
            ))}
          </div>

          <p className="mt-2 text-xs text-ink-400">Menunggu {order.waitingMinutes} menit</p>
        </div>

        <div className="mt-3 flex gap-2">
          {order.status === 'QUEUED' && (
              <button onClick={() => onCancel(order)} className="btn-danger flex-1 py-1.5 text-xs">Batal</button>
          )}
          {order.paymentStatus === 'PAID' ? (
              <span className="flex-1 inline-flex items-center justify-center gap-1 rounded-md bg-sprout-500/15 py-1.5 text-xs font-semibold text-sprout-600">
            ✓ Sudah Dibayar
          </span>
          ) : (
              <button onClick={() => onConfirmCash(order)} className="flex-1 text-xs font-medium text-ink-500 underline decoration-dotted">
                Konfirmasi tunai
              </button>
          )}
        </div>
      </div>
  )
}
function KanbanColumn({ status, label, orders, activeId, onCancel, onConfirmCash }) {
  const { setNodeRef, isOver } = useDroppable({ id: status })

  return (
    <div
      ref={setNodeRef}
      className={`flex min-w-[260px] flex-1 flex-col rounded-2xl border-2 border-dashed p-3 transition ${
        isOver ? 'border-ember-400 bg-ember-50/40' : 'border-ink-100 bg-ink-50/40'
      }`}
    >
      <div className="mb-3 flex items-center justify-between px-1">
        <h2 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-600">{label}</h2>
        <span className="rounded-full bg-white px-2 py-0.5 text-xs font-semibold text-ink-500 shadow-sm">
          {orders.length}
        </span>
      </div>

      <div className="flex flex-1 flex-col gap-3">
        {orders.length === 0 ? (
          <p className="mt-4 text-center text-xs text-ink-300">Kosong</p>
        ) : (
          orders.map((order) => (
            <OrderCard
              key={order.orderNumber}
              order={order}
              dragging={activeId === order.orderNumber}
              onCancel={onCancel}
              onConfirmCash={onConfirmCash}
            />
          ))
        )}
      </div>
    </div>
  )
}

export default function KitchenBoardPage() {
  const { data: board, isLoading } = useKitchenBoard()
  const updateStatus = useUpdateOrderStatus()
  const confirmCash = useConfirmCashPayment()
  const [activeId, setActiveId] = useState(null)

  const grouped = useMemo(() => {
    const map = { QUEUED: [], PROCESSING: [], READY: [], COMPLETED: [] }
    for (const order of board || []) {
      if (map[order.status]) map[order.status].push(order)
    }
    return map
  }, [board])

  const activeOrder = useMemo(() => (board || []).find((o) => o.orderNumber === activeId), [board, activeId])

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 150, tolerance: 8 } })
  )

  const handleDragEnd = async (event) => {
    const { active, over } = event
    setActiveId(null)
    if (!over) return

    const sourceStatus = active.data.current?.status
    const targetStatus = over.id
    if (sourceStatus === targetStatus) return

    try {
      await updateStatus.mutateAsync({ orderNumber: active.id, status: targetStatus })
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Perpindahan status tidak valid'))
    }
  }

  const handleCancel = async (order) => {
    try {
      await updateStatus.mutateAsync({ orderNumber: order.orderNumber, status: 'CANCELLED', note: 'Dibatalkan dari papan dapur' })
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal membatalkan'))
    }
  }

  const handleConfirmCash = (order) => {
    confirmCash.mutate(order.orderNumber)
  }

  return (
    <AdminLayout>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink-800">Papan Dapur</h1>
          <p className="text-sm text-ink-500">Seret kartu antar kolom untuk ubah status • update otomatis tiap 5 detik</p>
        </div>
      </div>

      {isLoading ? (
        <p className="mt-6 text-ink-400">Memuat papan antrean...</p>
      ) : (
        <DndContext
          sensors={sensors}
          onDragStart={(e) => setActiveId(e.active.id)}
          onDragEnd={handleDragEnd}
          onDragCancel={() => setActiveId(null)}
        >
          <div className="mt-6 flex gap-4 overflow-x-auto pb-2">
            {COLUMNS.map((col) => (
              <KanbanColumn
                key={col.status}
                status={col.status}
                label={col.label}
                orders={grouped[col.status]}
                activeId={activeId}
                onCancel={handleCancel}
                onConfirmCash={handleConfirmCash}
              />
            ))}
          </div>

          <DragOverlay>
            {activeOrder ? (
              <div className="card w-64 rotate-2 p-3 shadow-lg">
                <p className="font-display text-2xl font-bold text-ink-800">
                  {String(activeOrder.queueNumber ?? '-').padStart(2, '0')}
                </p>
                {activeOrder.customerName && <p className="text-xs font-medium text-ink-600">{activeOrder.customerName}</p>}
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      )}
    </AdminLayout>
  )
}
