import React, { useState } from 'react'
import toast from 'react-hot-toast'
import AdminLayout from '../../components/AdminLayout'
import { useCategories, useProducts } from '../../api/products'
import { useCreateProduct, useUpdateProduct, useDeactivateProduct, useAdjustStock } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'
import { formatIDR } from '../../utils/currency'

const EMPTY_FORM = { id: null, categoryId: '', name: '', description: '', price: '', stock: '', imageUrl: '' }

export default function ProductsPage() {
  const { data: categories } = useCategories()
  const { data: productPage, isLoading } = useProducts({ page: 0, size: 100 })
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()
  const deactivateProduct = useDeactivateProduct()
  const adjustStock = useAdjustStock()

  const [form, setForm] = useState(EMPTY_FORM)
  const [stockDelta, setStockDelta] = useState({})

  const isEditing = Boolean(form.id)

  const resetForm = () => setForm(EMPTY_FORM)

  const handleSubmit = async (e) => {
    e.preventDefault()
    const payload = {
      categoryId: Number(form.categoryId),
      name: form.name,
      description: form.description || undefined,
      price: Number(form.price),
      stock: Number(form.stock),
      imageUrl: form.imageUrl || undefined,
      active: true,
    }
    try {
      if (isEditing) {
        await updateProduct.mutateAsync({ id: form.id, payload })
        toast.success('Produk diperbarui')
      } else {
        await createProduct.mutateAsync(payload)
        toast.success('Produk ditambahkan')
      }
      resetForm()
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal menyimpan produk'))
    }
  }

  const handleEdit = (product) => {
    setForm({
      id: product.id,
      categoryId: String(product.categoryId),
      name: product.name,
      description: product.description || '',
      price: String(product.price),
      stock: String(product.stock),
      imageUrl: product.imageUrl || '',
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleDeactivate = async (id) => {
    try {
      await deactivateProduct.mutateAsync(id)
      toast.success('Produk dinonaktifkan')
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal menonaktifkan produk'))
    }
  }

  const handleAdjustStock = async (productId) => {
    const delta = Number(stockDelta[productId] || 0)
    if (!delta) return
    try {
      await adjustStock.mutateAsync({ productId, payload: { quantityChange: delta, note: 'Penyesuaian manual dashboard' } })
      toast.success('Stok diperbarui')
      setStockDelta((prev) => ({ ...prev, [productId]: '' }))
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Gagal menyesuaikan stok'))
    }
  }

  return (
    <AdminLayout>
      <h1 className="font-display text-2xl font-semibold text-ink-800">Produk & Stok</h1>
      <p className="text-sm text-ink-500">Kelola menu dan sesuaikan stok — setiap perubahan stok tercatat di ledger</p>

      <form onSubmit={handleSubmit} className="card mt-6 grid grid-cols-1 gap-4 p-5 sm:grid-cols-2">
        <div>
          <label className="label">Nama Produk</label>
          <input className="input" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </div>
        <div>
          <label className="label">Kategori</label>
          <select
            className="input"
            required
            value={form.categoryId}
            onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
          >
            <option value="">Pilih kategori</option>
            {categories?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">Harga (Rp)</label>
          <input
            type="number"
            min="0"
            className="input"
            required
            value={form.price}
            onChange={(e) => setForm({ ...form, price: e.target.value })}
          />
        </div>
        <div>
          <label className="label">Stok Awal</label>
          <input
            type="number"
            min="0"
            className="input"
            required
            disabled={isEditing}
            value={form.stock}
            onChange={(e) => setForm({ ...form, stock: e.target.value })}
          />
          {isEditing && <p className="mt-1 text-xs text-ink-400">Ubah stok lewat kolom "Sesuaikan Stok" di tabel.</p>}
        </div>
        <div className="sm:col-span-2">
          <label className="label">Deskripsi</label>
          <textarea
            className="input"
            rows={2}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
        <div className="flex gap-2 sm:col-span-2">
          <button type="submit" className="btn-primary">
            {isEditing ? 'Simpan Perubahan' : 'Tambah Produk'}
          </button>
          {isEditing && (
            <button type="button" onClick={resetForm} className="btn-secondary">
              Batal
            </button>
          )}
        </div>
      </form>

      <div className="card mt-6 overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-ink-50 text-left text-xs uppercase text-ink-500">
            <tr>
              <th className="px-4 py-3">Produk</th>
              <th className="px-4 py-3">Harga</th>
              <th className="px-4 py-3">Stok</th>
              <th className="px-4 py-3">Sesuaikan Stok</th>
              <th className="px-4 py-3">Aksi</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-ink-400">
                  Memuat...
                </td>
              </tr>
            )}
            {productPage?.content?.map((p) => (
              <tr key={p.id} className="border-t border-ink-100">
                <td className="px-4 py-3 font-medium text-ink-800">{p.name}</td>
                <td className="px-4 py-3">{formatIDR(p.price)}</td>
                <td className="px-4 py-3">
                  <span className={p.stock <= 5 ? 'font-semibold text-chili-600' : ''}>{p.stock}</span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      placeholder="±jumlah"
                      className="w-24 rounded-lg border border-ink-200 px-2 py-1 text-xs"
                      value={stockDelta[p.id] || ''}
                      onChange={(e) => setStockDelta((prev) => ({ ...prev, [p.id]: e.target.value }))}
                    />
                    <button onClick={() => handleAdjustStock(p.id)} className="btn-secondary px-2 py-1 text-xs">
                      Terapkan
                    </button>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button onClick={() => handleEdit(p)} className="text-xs font-medium text-ember-600">
                      Ubah
                    </button>
                    <button onClick={() => handleDeactivate(p.id)} className="text-xs font-medium text-chili-600">
                      Nonaktifkan
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  )
}
