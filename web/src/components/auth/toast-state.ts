import { reactive } from 'vue'

interface ToastItem {
  id: number
  message: string
  type: 'success' | 'error'
}

let nextId = 0

export const toasts = reactive<ToastItem[]>([])

export function showToast(message: string, type: 'success' | 'error' = 'error') {
  const id = nextId++
  toasts.push({ id, message, type })
  setTimeout(() => {
    const index = toasts.findIndex((t) => t.id === id)
    if (index > -1) toasts.splice(index, 1)
  }, 3000)
}
