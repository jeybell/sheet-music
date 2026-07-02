import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, register as registerApi } from '../apis/authApi'
import { AUTH_TOKEN_KEY, AUTH_USERNAME_KEY } from '../apis/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(AUTH_TOKEN_KEY))
  const username = ref<string | null>(localStorage.getItem(AUTH_USERNAME_KEY))
  const isAuthenticated = computed(() => !!token.value)

  const setSession = (t: string, u: string) => {
    token.value = t
    username.value = u
    localStorage.setItem(AUTH_TOKEN_KEY, t)
    localStorage.setItem(AUTH_USERNAME_KEY, u)
  }

  const login = async (loginUsername: string, password: string) => {
    const res = await loginApi(loginUsername, password)
    setSession(res.token, res.username)
  }

  const register = async (registerUsername: string, password: string) => {
    const res = await registerApi(registerUsername, password)
    setSession(res.token, res.username)
  }

  const logout = () => {
    token.value = null
    username.value = null
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USERNAME_KEY)
  }

  return { token, username, isAuthenticated, login, register, logout }
})
