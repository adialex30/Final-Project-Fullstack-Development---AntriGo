import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const apiClient = axios.create({ baseURL })

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('antrigo_admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('antrigo_admin_token')
      localStorage.removeItem('antrigo_admin_user')
    }
    return Promise.reject(error)
  }
)

export function extractErrorMessage(error, fallback = 'Terjadi kesalahan, coba lagi') {
  return error?.response?.data?.message || fallback
}
