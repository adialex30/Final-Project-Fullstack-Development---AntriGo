import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from './client'

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

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload) => (await apiClient.post('/admin/categories', payload)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

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
    onMutate: async ({ orderNumber, status }) => {
      await qc.cancelQueries({ queryKey: ['kitchen-board'] })
      const previous = qc.getQueryData(['kitchen-board'])
      qc.setQueryData(['kitchen-board'], (old) =>
        old?.map((o) => (o.orderNumber === orderNumber ? { ...o, status } : o))
      )
      return { previous }
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) qc.setQueryData(['kitchen-board'], context.previous)
    },
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