import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getSong, getSongs, type Song } from '../apis/songs'

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
    } catch {
      errorMessage.value = '곡 목록을 불러오지 못했습니다.'
    } finally {
      isLoading.value = false
    }
  }

  const fetchSong = async (id: number) => {
    isLoading.value = true
    errorMessage.value = ''
    selectedSong.value = null

    try {
      selectedSong.value = await getSong(id)
    } catch {
      errorMessage.value = '곡 정보를 불러오지 못했습니다.'
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
