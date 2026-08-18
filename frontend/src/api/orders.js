import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient } from './client'

export function useCheckout() {
  return useMutation({
    mutationFn: async (payload) => (await apiClient.post('/orders', payload)).data,
  })
}

export function useOrder(orderNumber, options = {}) {
  return useQuery({
    queryKey: ['order', orderNumber],
    queryFn: async () => (await apiClient.get(`/orders/${orderNumber}`)).data,
    enabled: Boolean(orderNumber),
    ...options,
  })
}

/** Polling status — TanStack Query refetchInterval, bukan WebSocket (keputusan versi awal). */
export function useOrderStatus(orderNumber) {
  return useQuery({
    queryKey: ['order-status', orderNumber],
    queryFn: async () => (await apiClient.get(`/orders/${orderNumber}/status`)).data,
    enabled: Boolean(orderNumber),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      if (status === 'COMPLETED' || status === 'CANCELLED') return false
      return 5000
    },
  })
}

export function useCancelOrder() {
  return useMutation({
    mutationFn: async (orderNumber) => (await apiClient.patch(`/orders/${orderNumber}/cancel`)).data,
  })
}

export function useConfirmQrisPayment() {
  return useMutation({
    mutationFn: async (orderNumber) =>
      (await apiClient.post(`/orders/${orderNumber}/payments/qris/confirm`)).data,
  })
}
