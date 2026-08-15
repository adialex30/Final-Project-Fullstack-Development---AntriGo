import { useMutation } from '@tanstack/react-query'
import { apiClient } from './client'

export function useLogin() {
  return useMutation({
    mutationFn: async ({ email, password }) =>
      (await apiClient.post('/auth/login', { email, password })).data,
  })
}
