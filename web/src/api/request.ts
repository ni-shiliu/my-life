import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { getToken, clearToken } from '@/utils/storage'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
