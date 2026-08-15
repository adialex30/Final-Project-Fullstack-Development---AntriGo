import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useLogin } from '../../api/auth'
import { useAuth } from '../../context/AuthContext'
import { extractErrorMessage } from '../../api/client'

export default function LoginPage() {
  const [email, setEmail] = useState('admin@antrigo.id')
  const [password, setPassword] = useState('')
  const login = useLogin()
  const { login: setAuth } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const response = await login.mutateAsync({ email, password })
      setAuth(response)
      toast.success(`Selamat datang, ${response.name}`)
      navigate('/admin/dashboard')
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Email atau password salah'))
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink-900 px-4">
      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded-2xl bg-paper p-7 shadow-xl">
        <p className="font-display text-2xl font-semibold text-ink-800">
          Antri<span className="text-ember-500">Go</span>
        </p>
        <p className="mt-1 text-sm text-ink-500">Masuk ke dashboard admin</p>

        <div className="mt-6 space-y-4">
          <div>
            <label className="label">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input"
              required
            />
          </div>
          <div>
            <label className="label">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input"
              required
            />
          </div>
        </div>

        <button type="submit" disabled={login.isPending} className="btn-primary mt-6 w-full">
          {login.isPending ? 'Memproses...' : 'Masuk'}
        </button>

        <p className="mt-4 text-center text-xs text-ink-400">
          Demo: admin@antrigo.id / Admin12345
        </p>
      </form>
    </div>
  )
}
