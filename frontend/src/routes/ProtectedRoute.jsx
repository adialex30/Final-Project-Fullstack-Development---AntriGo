import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const DEFAULT_ROUTE_BY_ROLE = {
  ADMIN: '/admin/dashboard',
  STAFF: '/admin/dashboard',
}

export default function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, user } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />
  }

  if (roles && !roles.includes(user?.role)) {
    return <Navigate to={DEFAULT_ROUTE_BY_ROLE[user?.role] || '/admin/dashboard'} replace />
  }

  return children
}