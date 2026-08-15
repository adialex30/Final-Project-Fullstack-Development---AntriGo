import React, { useState } from 'react'
import toast from 'react-hot-toast'
import CustomerNavbar from '../../components/CustomerNavbar'
import ProductCard from '../../components/ProductCard'
import { useCategories, useProducts } from '../../api/products'
import { useCart } from '../../context/CartContext'

export default function MenuPage() {
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState(null)
  const { data: categories } = useCategories()
  const { data: productPage, isLoading } = useProducts({ search, categoryId })
  const { addItem, tableNumber, setTableNumber } = useCart()

  const handleAdd = (product, variant, quantity) => {
    addItem(product, variant, quantity)
    toast.success(`${product.name} ditambahkan ke keranjang`)
  }

  return (
    <div className="min-h-screen">
      <CustomerNavbar />

      <div className="mx-auto max-w-3xl px-4 py-6">
        <div className="mb-6 rounded-2xl bg-ink-800 px-5 py-5 text-paper">
          <p className="text-xs uppercase tracking-wide text-ember-300">Selamat datang di</p>
          <h1 className="font-display text-2xl font-semibold">Warung AntriGo</h1>
          <div className="mt-3 flex items-center gap-2">
            <label className="text-sm text-ink-200">No. Meja</label>
            <input
              value={tableNumber}
              onChange={(e) => setTableNumber(e.target.value)}
              placeholder="mis. 07"
              className="w-20 rounded-lg border-0 bg-white/10 px-3 py-1.5 text-sm text-white placeholder:text-ink-300 focus:outline-none focus:ring-2 focus:ring-ember-400"
            />
          </div>
        </div>

        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari menu..."
          className="input mb-4"
        />

        <div className="mb-5 flex gap-2 overflow-x-auto pb-1">
          <button
            onClick={() => setCategoryId(null)}
            className={`whitespace-nowrap rounded-full px-4 py-1.5 text-sm font-medium ${
              categoryId === null ? 'bg-ink-800 text-white' : 'bg-white text-ink-600 border border-ink-200'
            }`}
          >
            Semua
          </button>
          {categories?.map((c) => (
            <button
              key={c.id}
              onClick={() => setCategoryId(c.id)}
              className={`whitespace-nowrap rounded-full px-4 py-1.5 text-sm font-medium ${
                categoryId === c.id ? 'bg-ink-800 text-white' : 'bg-white text-ink-600 border border-ink-200'
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>

        {isLoading && <p className="text-center text-ink-400">Memuat menu...</p>}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {productPage?.content?.map((product) => (
            <ProductCard key={product.id} product={product} onAdd={handleAdd} />
          ))}
        </div>

        {productPage && productPage.content.length === 0 && (
          <p className="mt-10 text-center text-ink-400">Menu tidak ditemukan.</p>
        )}
      </div>
    </div>
  )
}
