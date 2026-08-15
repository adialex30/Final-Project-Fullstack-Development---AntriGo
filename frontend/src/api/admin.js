import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from './client'

// ---- Products (admin CRUD) ----
export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload) => (await apiClient.post('/admin/products', payload)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  })
}

export function useUpdateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, payload }) => (await apiClient.put(`/admin/products/${id}`, payload)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  })
}

export function useDeactivateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`/admin/products/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  })
}

// ---- Categories (admin) ----
export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload) => (await apiClient.post('/admin/categories', payload)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

// ---- Stock ----
export function useAdjustStock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ productId, payload }) =>
      (await apiClient.post(`/admin/stock/${productId}/adjust`, payload)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['products'] })
      qc.invalidateQueries({ queryKey: ['low-stock'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

// ---- Kitchen board ----
export function useKitchenBoard() {
  return useQuery({
    queryKey: ['kitchen-board'],
    queryFn: async () => (await apiClient.get('/admin/orders/board')).data,
    refetchInterval: 5000,
  })
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ orderNumber, status, note }) =>
      (await apiClient.patch(`/admin/orders/${orderNumber}/status`, { status, note })).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['kitchen-board'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useConfirmCashPayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (orderNumber) =>
      (await apiClient.patch(`/admin/orders/${orderNumber}/payment/confirm`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['kitchen-board'] }),
  })
}

// ---- Reports ----
export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => (await apiClient.get('/admin/reports/dashboard')).data,
    refetchInterval: 30000,
  })
}

export function useLowStock() {
  return useQuery({
    queryKey: ['low-stock'],
    queryFn: async () => (await apiClient.get('/admin/reports/low-stock')).data,
  })
}
