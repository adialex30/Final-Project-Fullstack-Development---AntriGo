import { useMutation } from '@tanstack/react-query'
import { apiClient } from './client'

export function useLogin() {
  return useMutation({
    mutationFn: async ({ email, password }) =>
      (await apiClient.post('/auth/login', { email, password })).data,
  })
}

export function useRegisterStaff() {
  return useMutation({
    mutationFn: async ({ name, email, password, role }) =>
        (await apiClient.post('/auth/register', { name, email, password, role })).data,
  })
}