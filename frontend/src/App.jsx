import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import MenuPage from './pages/customer/MenuPage'
import CartPage from './pages/customer/CartPage'
import CheckoutPage from './pages/customer/CheckoutPage'
import QueueStatusPage from './pages/customer/QueueStatusPage'
import LoginPage from './pages/admin/LoginPage'
import DashboardPage from './pages/admin/DashboardPage'
import KitchenBoardPage from './pages/admin/KitchenBoardPage'
import ProductsPage from './pages/admin/ProductsPage'
import ReportsPage from './pages/admin/ReportsPage'
import ProtectedRoute from './routes/ProtectedRoute'
import RegisterStaffPage from './pages/admin/RegisterStaffPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MenuPage />} />
      <Route path="/cart" element={<CartPage />} />
      <Route path="/checkout" element={<CheckoutPage />} />
      <Route path="/queue/:orderNumber" element={<QueueStatusPage />} />

      <Route path="/admin/login" element={<LoginPage />} />
      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
        <Route
            path="/admin/register-staff"
            element={
              <ProtectedRoute>
                  <RegisterStaffPage />
              </ProtectedRoute>
        }
        />
      <Route
        path="/admin/board"
        element={
          <ProtectedRoute>
            <KitchenBoardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/products"
        element={
          <ProtectedRoute>
            <ProductsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/reports"
        element={
          <ProtectedRoute>
            <ReportsPage />
          </ProtectedRoute>
        }
      />

      <Route path="*" element=  {
          <Navigate to="/" replace />
      }
      />

        <Route
            path="/admin/dashboard"
            element={
                <ProtectedRoute roles={['ADMIN', 'STAFF']}>
                    <DashboardPage />
                </ProtectedRoute>
            }
        />
        <Route
            path="/admin/board"
            element={
                <ProtectedRoute roles={['STAFF']}>
                    <KitchenBoardPage />
                </ProtectedRoute>
            }
        />
        <Route
            path="/admin/products"
            element={
                <ProtectedRoute roles={['ADMIN']}>
                    <ProductsPage />
                </ProtectedRoute>
            }
        />
        <Route
            path="/admin/reports"
            element={
                <ProtectedRoute roles={['ADMIN']}>
                    <ReportsPage />
                </ProtectedRoute>
            }
        />
        <Route
            path="/admin/register-staff"
            element={
                <ProtectedRoute roles={['ADMIN']}>
                    <RegisterStaffPage />
                </ProtectedRoute>
            }
        />
      </Routes>

  )
}

