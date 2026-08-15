import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'

export default function CustomerNavbar() {
  const { totalQuantity } = useCart()
  const navigate = useNavigate()

  return (
    <header className="sticky top-0 z-20 border-b border-ink-100 bg-paper/90 backdrop-blur">
      <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
        <Link to="/" className="font-display text-xl font-semibold text-ink-800">
          Antri<span className="text-ember-500">Go</span>
        </Link>
        <button
          onClick={() => navigate('/cart')}
          className="relative rounded-full border border-ink-200 bg-white px-4 py-2 text-sm font-semibold text-ink-700"
        >
          Keranjang
          {totalQuantity > 0 && (
            <span className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-chili-500 text-xs font-bold text-white">
              {totalQuantity}
            </span>
          )}
        </button>
      </div>
    </header>
  )
}
