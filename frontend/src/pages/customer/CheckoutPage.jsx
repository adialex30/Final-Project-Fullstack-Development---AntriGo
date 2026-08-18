import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import CustomerNavbar from '../../components/CustomerNavbar'
import QrisPaymentModal from '../../components/QrisPaymentModal'
import { useCart } from '../../context/CartContext'
import { useCheckout, useCancelOrder, useOrder, useOrderStatus } from '../../api/orders'
import { extractErrorMessage } from '../../api/client'
import { formatIDR } from '../../utils/currency'

const PENDING_SESSION_KEY = 'antrigo_pending_qris_order'
const PHONE_PATTERN = /^[0-9+][0-9\-+\s]{7,19}$/

export default function CheckoutPage() {
  const { items, subtotal, tableNumber, customerName, setCustomerName, customerPhone, setCustomerPhone, toCheckoutItems, clearCart } = useCart()
  const [paymentMethod, setPaymentMethod] = useState('QRIS')
  const [note, setNote] = useState('')
  const [qrisSession, setQrisSession] = useState(null)
  const navigate = useNavigate()

  const checkout = useCheckout()
  const cancelOrder = useCancelOrder()

  const pendingOrderNumber = typeof window !== 'undefined' ? sessionStorage.getItem(PENDING_SESSION_KEY) : null
  const { data: resumedOrder } = useOrder(pendingOrderNumber, { enabled: Boolean(pendingOrderNumber) && !qrisSession })

  useEffect(() => {
    if (!resumedOrder) return
    if (resumedOrder.status === 'AWAITING_PAYMENT' && resumedOrder.qrPayload) {
      setQrisSession({
        orderNumber: resumedOrder.orderNumber,
        amount: resumedOrder.totalAmount,
        qrPayload: resumedOrder.qrPayload,
        paymentExpiresAt: resumedOrder.paymentExpiresAt,
      })
    } else {
      sessionStorage.removeItem(PENDING_SESSION_KEY)
    }
  }, [resumedOrder])

  const qrisOrderStatus = useOrderStatus(qrisSession?.orderNumber)

  useEffect(() => {
    if (!qrisSession) return
    if (qrisOrderStatus.data?.status === 'QUEUED') {
      const orderNumber = qrisSession.orderNumber
      sessionStorage.removeItem(PENDING_SESSION_KEY)
      setQrisSession(null)
      clearCart()
      toast.success('Pembayaran dikonfirmasi — pesanan masuk antrean!')
      navigate(`/queue/${orderNumber}`)
    }
  }, [qrisOrderStatus.data?.status, qrisSession])

  const validateCustomerInfo = () => {
    if (!customerName.trim()) { toast.error('Nama wajib diisi'); return false }
    if (!PHONE_PATTERN.test(customerPhone.trim())) { toast.error('Nomor telepon tidak valid'); return false }
    return true
  }

  const handleSubmit = async () => {
    if (items.length === 0) { toast.error('Keranjang masih kosong'); return }
    if (!validateCustomerInfo()) return

    try {
      const order = await checkout.mutateAsync({
        tableNumber: tableNumber || undefined,
        customerName: customerName.trim(),
        customerPhone: customerPhone.trim(),
        items: toCheckoutItems(),
        paymentMethod,
        note: note || undefined,
      })

      if (paymentMethod === 'QRIS') {
        sessionStorage.setItem(PENDING_SESSION_KEY, order.orderNumber)
        setQrisSession({
          orderNumber: order.orderNumber,
          amount: order.totalAmount,
          qrPayload: order.qrPayload,
          paymentExpiresAt: order.paymentExpiresAt,
        })
        return
      }

      clearCart()
      toast.success('Pesanan berhasil dibuat!')
      navigate(`/queue/${order.orderNumber}`)
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Checkout gagal, coba lagi'))
    }
  }

  const handleQrisClose = async () => {
    const orderNumber = qrisSession?.orderNumber
    sessionStorage.removeItem(PENDING_SESSION_KEY)
    setQrisSession(null)
    if (orderNumber) cancelOrder.mutate(orderNumber, { onError: () => {} })
  }

  return (
      <div className="min-h-screen">
        <CustomerNavbar />
        <div className="mx-auto max-w-3xl px-4 py-6">
          <h1 className="font-display text-2xl font-semibold text-ink-800">Pembayaran</h1>

          <div className="card mt-5 p-4">
            <p className="label">Data Pemesan</p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="label">Nama</label>
                <input value={customerName} onChange={(e) => setCustomerName(e.target.value)} className="input" placeholder="mis. Budi Santoso" />
              </div>
              <div>
                <label className="label">Nomor Telepon</label>
                <input value={customerPhone} onChange={(e) => setCustomerPhone(e.target.value)} className="input" placeholder="mis. 08123456789" inputMode="tel" />
              </div>
            </div>
          </div>

          <div className="card mt-4 p-4">
            <p className="label">Metode Pembayaran</p>
            <div className="grid grid-cols-2 gap-3">
              <button onClick={() => setPaymentMethod('QRIS')} className={`rounded-xl border-2 p-4 text-left ${paymentMethod === 'QRIS' ? 'border-ember-500 bg-ember-50' : 'border-ink-200'}`}>
                <p className="font-semibold text-ink-800">QRIS</p>
                <p className="text-xs text-ink-500">Bayar instan lewat QR</p>
              </button>
              <button onClick={() => setPaymentMethod('CASH')} className={`rounded-xl border-2 p-4 text-left ${paymentMethod === 'CASH' ? 'border-ember-500 bg-ember-50' : 'border-ink-200'}`}>
                <p className="font-semibold text-ink-800">Tunai</p>
                <p className="text-xs text-ink-500">Bayar di kasir</p>
              </button>
            </div>
          </div>

          <div className="card mt-4 p-4">
            <label className="label">Catatan (opsional)</label>
            <textarea value={note} onChange={(e) => setNote(e.target.value)} className="input" rows={2} placeholder="mis. tanpa bawang, sendok terpisah" />
          </div>

          <div className="card mt-4 space-y-2 p-4">
            {items.map((item) => (
                <div key={`${item.productId}-${item.variantId}`} className="flex justify-between text-sm">
                  <span className="text-ink-600">{item.quantity}× {item.productName}{item.variantName ? ` (${item.variantName})` : ''}</span>
                  <span className="font-medium text-ink-800">{formatIDR(item.unitPrice * item.quantity)}</span>
                </div>
            ))}
            <div className="flex justify-between border-t border-ink-100 pt-2 font-semibold text-ink-800">
              <span>Total</span>
              <span>{formatIDR(subtotal)}</span>
            </div>
          </div>

          <button onClick={handleSubmit} disabled={checkout.isPending} className="btn-primary mt-5 w-full py-3 text-base">
            {checkout.isPending ? 'Memproses...' : `Bayar ${formatIDR(subtotal)}`}
          </button>
        </div>

        <QrisPaymentModal
            open={Boolean(qrisSession)}
            orderNumber={qrisSession?.orderNumber}
            amount={qrisSession?.amount ?? subtotal}
            qrPayload={qrisSession?.qrPayload}
            expiresAt={qrisSession?.paymentExpiresAt}
            onClose={handleQrisClose}
        />
      </div>
  )
}