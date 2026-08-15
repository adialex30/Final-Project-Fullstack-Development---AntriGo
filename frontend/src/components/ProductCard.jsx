import React, { useState } from 'react'
import { formatIDR } from '../utils/currency'

export default function ProductCard({ product, onAdd }) {
  const [variantId, setVariantId] = useState(product.variants?.[0]?.id ?? null)
  const [quantity, setQuantity] = useState(1)
  const outOfStock = product.stock <= 0

  const selectedVariant = product.variants?.find((v) => v.id === variantId) || null
  const displayPrice = Number(product.price) + Number(selectedVariant?.priceDelta || 0)

  return (
    <div className="card flex flex-col gap-3 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-display text-lg font-semibold text-ink-800">{product.name}</h3>
          {product.description && (
            <p className="mt-0.5 text-sm text-ink-500 line-clamp-2">{product.description}</p>
          )}
        </div>
        <span className="whitespace-nowrap font-display text-lg font-semibold text-ember-600">
          {formatIDR(displayPrice)}
        </span>
      </div>

      {product.variants?.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {product.variants.map((v) => (
            <button
              key={v.id}
              onClick={() => setVariantId(v.id)}
              className={`rounded-full border px-3 py-1 text-xs font-medium transition ${
                variantId === v.id
                  ? 'border-ember-500 bg-ember-50 text-ember-700'
                  : 'border-ink-200 text-ink-600 hover:border-ink-300'
              }`}
            >
              {v.name}
            </button>
          ))}
        </div>
      )}

      <div className="flex items-center justify-between pt-1">
        {outOfStock ? (
          <span className="text-sm font-semibold text-chili-600">Stok habis</span>
        ) : (
          <span className="text-xs text-ink-400">Sisa stok: {product.stock}</span>
        )}

        <div className="flex items-center gap-2">
          <div className="flex items-center rounded-full border border-ink-200">
            <button
              className="px-3 py-1 text-ink-500 disabled:opacity-30"
              disabled={outOfStock}
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            >
              −
            </button>
            <span className="w-6 text-center text-sm font-semibold">{quantity}</span>
            <button
              className="px-3 py-1 text-ink-500 disabled:opacity-30"
              disabled={outOfStock}
              onClick={() => setQuantity((q) => Math.min(product.stock, q + 1))}
            >
              +
            </button>
          </div>
          <button
            className="btn-primary px-4 py-1.5 text-sm"
            disabled={outOfStock}
            onClick={() => onAdd(product, selectedVariant, quantity)}
          >
            Tambah
          </button>
        </div>
      </div>
    </div>
  )
}
