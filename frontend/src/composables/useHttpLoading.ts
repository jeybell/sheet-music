import { ref } from 'vue'
import http from '../apis/http'

const pendingCount = ref(0)
export const isLoading = () => pendingCount

let interceptorsRegistered = false

export function setupHttpLoadingInterceptors() {
  if (interceptorsRegistered) return
  interceptorsRegistered = true

  http.interceptors.request.use((config) => {
    pendingCount.value++
    return config
  })

  http.interceptors.response.use(
    (response) => {
      pendingCount.value = Math.max(0, pendingCount.value - 1)
      return response
    },
    (error) => {
      pendingCount.value = Math.max(0, pendingCount.value - 1)
      return Promise.reject(error)
    },
  )
}
