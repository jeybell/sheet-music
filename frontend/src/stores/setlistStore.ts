import { defineStore } from 'pinia'
import { ref } from 'vue'
import { isAxiosError } from 'axios'
import { getSetlist, getSetlists } from '../apis/setlistApi'
import type { Setlist } from '../types/setlist'

interface ApiErrorResponse {
  message?: string
}

const apiError = (error: unknown, fallback: string) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return fallback
}

export const useSetlistStore = defineStore('setlist', () => {
  const setlists = ref<Setlist[]>([])
  const selectedSetlist = ref<Setlist | null>(null)
  const isLoading = ref(false)
  const errorMessage = ref('')

  const fetchSetlists = async () => {
    isLoading.value = true
    errorMessage.value = ''
    try {
      setlists.value = await getSetlists()
    } catch (e) {
      errorMessage.value = apiError(e, '셋리스트를 불러오지 못했습니다.')
    } finally {
      isLoading.value = false
    }
  }

  const fetchSetlist = async (setlistId: number) => {
    isLoading.value = true
    errorMessage.value = ''
    selectedSetlist.value = null
    try {
      selectedSetlist.value = await getSetlist(setlistId)
    } catch (e) {
      errorMessage.value = apiError(e, '셋리스트를 불러오지 못했습니다.')
    } finally {
      isLoading.value = false
    }
  }

  return { setlists, selectedSetlist, isLoading, errorMessage, fetchSetlists, fetchSetlist }
})
