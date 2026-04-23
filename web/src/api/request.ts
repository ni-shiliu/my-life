import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { getToken, clearToken } from '@/utils/storage'
import { showToast } from '@/components/auth/toast-state'
import router from '@/router'

const TOKEN_ERROR_CODES = ['TOKEN_EXPIRED', 'TOKEN_INVALID']

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
  (response) => {
    const data = response.data
    // 只有当响应体包含 code 字段时才走业务码判断
    if (data && 'code' in data) {
      if (data.code === '0' || data.code === 0) {
        return response
      }
      if (TOKEN_ERROR_CODES.includes(data.code)) {
        handleAuthExpired()
        return Promise.reject(new Error(data.message || '登录已过期'))
      }
      const msg = data.message || '请求失败'
      showToast(msg, 'error')
      return Promise.reject(new Error(msg))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      handleAuthExpired()
    } else {
      const msg = error.response?.data?.message || error.message || '网络异常，请稍后重试'
      showToast(msg, 'error')
    }
    return Promise.reject(error)
  }
)

let isRedirecting = false

function handleAuthExpired() {
  if (isRedirecting) return
  isRedirecting = true
  clearToken()
  showToast('登录已失效，请重新登录', 'error')
  setTimeout(() => {
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } }).finally(() => {
      isRedirecting = false
    })
  }, 1500)
}

export default request
