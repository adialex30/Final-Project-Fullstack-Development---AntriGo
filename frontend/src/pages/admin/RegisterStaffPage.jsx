import React, { useState } from 'react'
import toast from 'react-hot-toast'
import AdminLayout from '../../components/AdminLayout'
import { useRegisterStaff } from '../../api/auth'
import { extractErrorMessage } from '../../api/client'

export default function RegisterStaffPage() {
    const [name, setName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [role, setRole] = useState('STAFF')
    const registerStaff = useRegisterStaff()

    const handleSubmit = async (e) => {
        e.preventDefault()
        try {
            await registerStaff.mutateAsync({ name, email, password, role })
            toast.success(`Akun ${role === 'ADMIN' ? 'admin' : 'staff'} "${name}" berhasil dibuat`)
            setName('')
            setEmail('')
            setPassword('')
            setRole('STAFF')
        } catch (err) {
            toast.error(extractErrorMessage(err, 'Gagal membuat akun'))
        }
    }

    return (
        <AdminLayout>
            <h1 className="font-display text-2xl font-semibold text-ink-800">Registrasi Staff</h1>
            <p className="text-sm text-ink-500">Buat akun staff atau admin baru</p>

            <form onSubmit={handleSubmit} className="card mt-6 max-w-md space-y-4 p-5">
                <div>
                    <label className="label">Nama</label>
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="input"
                        required
                    />
                </div>
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
                        minLength={8}
                        required
                    />
                </div>
                <div>
                    <label className="label">Role</label>
                    <select value={role} onChange={(e) => setRole(e.target.value)} className="input">
                        <option value="STAFF">Staff</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                </div>

                <button type="submit" disabled={registerStaff.isPending} className="btn-primary w-full">
                    {registerStaff.isPending ? 'Memproses...' : 'Buat Akun'}
                </button>
            </form>
        </AdminLayout>
    )
}