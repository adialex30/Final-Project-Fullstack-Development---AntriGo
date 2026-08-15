import { useQuery } from '@tanstack/react-query'
import { apiClient } from './client'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await apiClient.get('/categories')).data,
  })
}

export function useProducts({ search, categoryId, page = 0, size = 20 }) {
  return useQuery({
    queryKey: ['products', search, categoryId, page, size],
    queryFn: async () => {
      const params = { page, size }
      if (search) params.search = search
      if (categoryId) params.category = categoryId
      return (await apiClient.get('/products', { params })).data
    },
    placeholderData: (prev) => prev,
  })
}
