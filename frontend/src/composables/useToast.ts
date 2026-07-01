import { ref } from 'vue'

export interface Toast {
  id: number
  message: string
  type: 'success' | 'error'
}

// 앱 전역에서 공유하는 토스트 큐 (모듈 싱글턴)
const toasts = ref<Toast[]>([])
let seq = 0

const dismiss = (id: number) => {
  const idx = toasts.value.findIndex(t => t.id === id)
  if (idx !== -1) toasts.value.splice(idx, 1)
}

const show = (message: string, type: Toast['type']) => {
  const id = ++seq
  toasts.value.push({ id, message, type })
  setTimeout(() => dismiss(id), 2500)
}

export function useToast() {
  return {
    toasts,
    success: (message: string) => show(message, 'success'),
    error: (message: string) => show(message, 'error'),
    dismiss,
  }
}
