import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import CustomerNavbar from '../../components/CustomerNavbar'
import { useCart } from '../../context/CartContext'
import { useCheckout } from '../../api/orders'
import { extractErrorMessage } from '../../api/client'
import { formatIDR } from '../../utils/currency'

export default function CheckoutPage() {
  const { items, subtotal, tableNumber, toCheckoutItems, clearCart } = useCart()
  const [paymentMethod, setPaymentMethod] = useState('QRIS')
  const [note, setNote] = useState('')
  const checkout = useCheckout()
  const navigate = useNavigate()

  const handleSubmit = async () => {
    if (items.length === 0) {
      toast.error('Keranjang masih kosong')
      return
    }
    try {
      const order = await checkout.mutateAsync({
        tableNumber: tableNumber || undefined,
        items: toCheckoutItems(),
        paymentMethod,
        note: note || undefined,
      })
      clearCart()
      toast.success('Pesanan berhasil dibuat!')
      navigate(`/queue/${order.orderNumber}`)
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Checkout gagal, coba lagi'))
    }
  }

  return (
    <div className="min-h-screen">
      <CustomerNavbar />
      <div className="mx-auto max-w-3xl px-4 py-6">
        <h1 className="font-display text-2xl font-semibold text-ink-800">Pembayaran</h1>

        <div className="card mt-5 p-4">
          <p className="label">Metode Pembayaran</p>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setPaymentMethod('QRIS')}
              className={`rounded-xl border-2 p-4 text-left ${
                paymentMethod === 'QRIS' ? 'border-ember-500 bg-ember-50' : 'border-ink-200'
              }`}
            >
              <p className="font-semibold text-ink-800">QRIS</p>
              <p className="text-xs text-ink-500">Bayar instan lewat QR</p>
            </button>
            <button
              onClick={() => setPaymentMethod('CASH')}
              className={`rounded-xl border-2 p-4 text-left ${
                paymentMethod === 'CASH' ? 'border-ember-500 bg-ember-50' : 'border-ink-200'
              }`}
            >
              <p className="font-semibold text-ink-800">Tunai</p>
              <p className="text-xs text-ink-500">Bayar di kasir</p>
            </button>
          </div>
        </div>

        <div className="card mt-4 p-4">
          <label className="label">Catatan (opsional)</label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            className="input"
            rows={2}
            placeholder="mis. tanpa bawang, sendok terpisah"
          />
        </div>

        <div className="card mt-4 space-y-2 p-4">
          {items.map((item) => (
            <div key={`${item.productId}-${item.variantId}`} className="flex justify-between text-sm">
              <span className="text-ink-600">
                {item.quantity}× {item.productName}
                {item.variantName ? ` (${item.variantName})` : ''}
              </span>
              <span className="font-medium text-ink-800">{formatIDR(item.unitPrice * item.quantity)}</span>
            </div>
          ))}
          <div className="flex justify-between border-t border-ink-100 pt-2 font-semibold text-ink-800">
            <span>Total</span>
            <span>{formatIDR(subtotal)}</span>
          </div>
        </div>

        <button
          onClick={handleSubmit}
          disabled={checkout.isPending}
          className="btn-primary mt-5 w-full py-3 text-base"
        >
          {checkout.isPending ? 'Memproses...' : `Bayar ${formatIDR(subtotal)}`}
        </button>
      </div>
    </div>
  )
}
