import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('antrigo_admin_user')
    return raw ? JSON.parse(raw) : null
  })

  useEffect(() => {
    if (user) {
      localStorage.setItem('antrigo_admin_user', JSON.stringify(user))
    } else {
      localStorage.removeItem('antrigo_admin_user')
    }
  }, [user])

  const login = useCallback((jwtResponse) => {
    localStorage.setItem('antrigo_admin_token', jwtResponse.token)
    const nextUser = { name: jwtResponse.name, email: jwtResponse.email, role: jwtResponse.role }
    setUser(nextUser)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('antrigo_admin_token')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: Boolean(user) }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth harus dipakai di dalam AuthProvider')
  return ctx
}
