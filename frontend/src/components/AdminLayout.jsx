import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const NAV_ITEMS = [
  { to: '/admin/dashboard', label: 'Dashboard' },
  { to: '/admin/board', label: 'Papan Dapur' },
  { to: '/admin/products', label: 'Produk & Stok' },
  { to: '/admin/reports', label: 'Laporan' },
]

export default function AdminLayout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/admin/login')
  }

  return (
    <div className="min-h-screen bg-ink-900 text-paper">
      <div className="flex">
        <aside className="hidden w-60 flex-col border-r border-ink-700 p-5 md:flex">
          <p className="font-display text-xl font-semibold">
            Antri<span className="text-ember-400">Go</span>
          </p>
          <p className="mt-0.5 text-xs text-ink-400">Admin Dashboard</p>

          <nav className="mt-8 flex flex-col gap-1">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded-lg px-3 py-2 text-sm font-medium transition ${
                    isActive ? 'bg-ember-500/15 text-ember-300' : 'text-ink-300 hover:bg-ink-800'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="mt-auto pt-6">
            <p className="text-xs text-ink-400">{user?.name}</p>
            <p className="text-[11px] text-ink-500">{user?.role}</p>
            <button
              onClick={handleLogout}
              className="mt-3 w-full rounded-lg border border-ink-700 px-3 py-1.5 text-xs font-medium text-ink-300 hover:bg-ink-800"
            >
              Keluar
            </button>
          </div>
        </aside>

        <main className="min-h-screen flex-1 bg-paper text-ink-800">
          <div className="mx-auto max-w-6xl px-5 py-6 md:px-8">{children}</div>
        </main>
      </div>
    </div>
  )
}
