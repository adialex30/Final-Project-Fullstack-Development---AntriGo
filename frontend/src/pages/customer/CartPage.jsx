import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import CustomerNavbar from '../../components/CustomerNavbar'
import { useCart } from '../../context/CartContext'
import { formatIDR } from '../../utils/currency'

export default function CartPage() {
  const { items, updateQuantity, removeItem, subtotal, tableNumber } = useCart()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen">
      <CustomerNavbar />
      <div className="mx-auto max-w-3xl px-4 py-6">
        <h1 className="font-display text-2xl font-semibold text-ink-800">Keranjang</h1>

        {items.length === 0 ? (
          <div className="mt-10 text-center">
            <p className="text-ink-400">Keranjang masih kosong.</p>
            <Link to="/" className="btn-primary mt-4 inline-flex">
              Lihat Menu
            </Link>
          </div>
        ) : (
          <>
            <div className="mt-5 space-y-3">
              {items.map((item) => (
                <div key={`${item.productId}-${item.variantId}`} className="card flex items-center justify-between p-4">
                  <div>
                    <p className="font-semibold text-ink-800">{item.productName}</p>
                    {item.variantName && <p className="text-xs text-ink-500">{item.variantName}</p>}
                    <p className="text-sm text-ember-600">{formatIDR(item.unitPrice)}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center rounded-full border border-ink-200">
                      <button
                        className="px-3 py-1 text-ink-500"
                        onClick={() => updateQuantity(item.productId, item.variantId, item.quantity - 1)}
                      >
                        −
                      </button>
                      <span className="w-6 text-center text-sm font-semibold">{item.quantity}</span>
                      <button
                        className="px-3 py-1 text-ink-500"
                        onClick={() => updateQuantity(item.productId, item.variantId, item.quantity + 1)}
                      >
                        +
                      </button>
                    </div>
                    <button
                      onClick={() => removeItem(item.productId, item.variantId)}
                      className="text-xs font-medium text-chili-600"
                    >
                      Hapus
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="card mt-6 space-y-3 p-4">
              <div className="flex items-center justify-between text-sm text-ink-500">
                <span>No. Meja</span>
                <span className="font-semibold text-ink-700">{tableNumber || '-'}</span>
              </div>
              <div className="flex items-center justify-between text-lg font-semibold text-ink-800">
                <span>Subtotal</span>
                <span>{formatIDR(subtotal)}</span>
              </div>
              <p className="text-xs text-ink-400">
                Harga final akan dihitung ulang oleh server saat checkout.
              </p>
              <button onClick={() => navigate('/checkout')} className="btn-primary w-full">
                Lanjut ke Pembayaran
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
