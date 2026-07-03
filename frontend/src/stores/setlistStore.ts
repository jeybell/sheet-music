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

  // 빠른 화면 전환 시(콘티 A→B) 느린 A 응답이 나중에 도착해 B 화면을 덮어쓰는 걸 막기 위해
  // 요청 순번을 매겨 가장 마지막 요청의 결과만 반영한다.
  let fetchSeq = 0
  const fetchSetlist = async (setlistId: number) => {
    const seq = ++fetchSeq
    isLoading.value = true
    errorMessage.value = ''
    selectedSetlist.value = null
    try {
      const result = await getSetlist(setlistId)
      if (seq !== fetchSeq) return
      selectedSetlist.value = result
    } catch (e) {
      if (seq !== fetchSeq) return
      errorMessage.value = apiError(e, '셋리스트를 불러오지 못했습니다.')
    } finally {
      if (seq === fetchSeq) isLoading.value = false
    }
  }

  return { setlists, selectedSetlist, isLoading, errorMessage, fetchSetlists, fetchSetlist }
})
