import axios from 'axios'
import { getActiveBaseUrl, isPrimaryBaseUrl, switchToFallbackBaseUrl } from './apiBase'

export const AUTH_TOKEN_KEY = 'auth_token'
export const AUTH_USERNAME_KEY = 'auth_username'
export const AUTH_ROLE_KEY = 'auth_role'

const http = axios.create({
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  config.baseURL = getActiveBaseUrl()
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
      localStorage.removeItem(AUTH_USERNAME_KEY)
      localStorage.removeItem(AUTH_ROLE_KEY)
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
      return Promise.reject(error)
    }

    // 1차 백엔드가 아예 응답하지 않는 경우(서버 다운/절전)에만 폴백으로 전환 후 재시도.
    // 4xx/5xx처럼 서버가 살아있는데 에러를 준 경우는 폴백 대상이 아니다.
    const config = error.config
    if (!error.response && config && isPrimaryBaseUrl(config.baseURL) && switchToFallbackBaseUrl()) {
      return http(config)
    }

    return Promise.reject(error)
  },
)

export default http
