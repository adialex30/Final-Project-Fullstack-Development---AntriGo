import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'

const CartContext = createContext(null)
const STORAGE_KEY = 'antrigo_cart'

/**
 * Keranjang di sisi client HANYA untuk pengalaman belanja (tampilkan harga, hitung subtotal
 * perkiraan). Saat checkout, payload yang dikirim ke server hanya berisi productId, variantId,
 * quantity, note — harga di sini tidak pernah dikirim; server menghitung ulang dari database.
 */
export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? JSON.parse(raw) : []
    } catch {
      return []
    }
  })
  const [tableNumber, setTableNumber] = useState(() => localStorage.getItem('antrigo_table') || '')

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  }, [items])

  useEffect(() => {
    localStorage.setItem('antrigo_table', tableNumber)
  }, [tableNumber])

  const cartKey = (productId, variantId) => `${productId}-${variantId ?? 'none'}`

  const addItem = (product, variant, quantity, note) => {
    const unitPrice = Number(product.price) + Number(variant?.priceDelta || 0)
    const key = cartKey(product.id, variant?.id)
    setItems((prev) => {
      const existing = prev.find((i) => cartKey(i.productId, i.variantId) === key)
      if (existing) {
        return prev.map((i) =>
          cartKey(i.productId, i.variantId) === key ? { ...i, quantity: i.quantity + quantity } : i
        )
      }
      return [
        ...prev,
        {
          productId: product.id,
          productName: product.name,
          variantId: variant?.id ?? null,
          variantName: variant?.name ?? null,
          unitPrice,
          quantity,
          note: note || '',
        },
      ]
    })
  }

  const updateQuantity = (productId, variantId, quantity) => {
    const key = cartKey(productId, variantId)
    setItems((prev) =>
      quantity <= 0
        ? prev.filter((i) => cartKey(i.productId, i.variantId) !== key)
        : prev.map((i) => (cartKey(i.productId, i.variantId) === key ? { ...i, quantity } : i))
    )
  }

  const removeItem = (productId, variantId) => {
    const key = cartKey(productId, variantId)
    setItems((prev) => prev.filter((i) => cartKey(i.productId, i.variantId) !== key))
  }

  const clearCart = () => setItems([])

  const subtotal = useMemo(
    () => items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0),
    [items]
  )
  const totalQuantity = useMemo(() => items.reduce((sum, i) => sum + i.quantity, 0), [items])

  /** Bentuk payload yang benar-benar dikirim ke backend — tanpa harga. */
  const toCheckoutItems = () =>
    items.map((i) => ({
      productId: i.productId,
      variantId: i.variantId,
      quantity: i.quantity,
      note: i.note || undefined,
    }))

  return (
    <CartContext.Provider
      value={{
        items,
        addItem,
        updateQuantity,
        removeItem,
        clearCart,
        subtotal,
        totalQuantity,
        toCheckoutItems,
        tableNumber,
        setTableNumber,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart harus dipakai di dalam CartProvider')
  return ctx
}
