import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { isAxiosError } from 'axios'
import { getSong, getSongs } from '../apis/songApi'
import type { Song } from '../types/song'

interface ApiErrorResponse {
  message?: string
}

const getErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallbackMessage
  }

  return fallbackMessage
}

export const useSongStore = defineStore('song', () => {
  const songs = ref<Song[]>([])
  const selectedSong = ref<Song | null>(null)
  const isLoading = ref(false)
  const errorMessage = ref('')

  const hasSongs = computed(() => songs.value.length > 0)

  const fetchSongs = async () => {
    isLoading.value = true
    errorMessage.value = ''

    try {
      songs.value = await getSongs()
    } catch (error) {
      errorMessage.value = getErrorMessage(error, '곡 목록을 불러오지 못했습니다.')
    } finally {
      isLoading.value = false
    }
  }

  const fetchSong = async (songId: number) => {
    isLoading.value = true
    errorMessage.value = ''
    selectedSong.value = null

    try {
      selectedSong.value = await getSong(songId)
    } catch (error) {
      errorMessage.value = getErrorMessage(error, '곡 정보를 불러오지 못했습니다.')
    } finally {
      isLoading.value = false
    }
  }

  return {
    songs,
    selectedSong,
    isLoading,
    errorMessage,
    hasSongs,
    fetchSongs,
    fetchSong,
  }
})
