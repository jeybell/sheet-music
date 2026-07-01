import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { isAxiosError } from 'axios'
import { getSong, getSongs, getSongsPage, type SongSearchParams } from '../apis/songApi'
import type { Song } from '../types/song'

const PAGE_SIZE = 20

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

  // ── 곡 목록 화면 무한 스크롤용 페이지 상태 (피커용 songs 와 분리)
  const listSongs = ref<Song[]>([])
  const listTotal = ref(0)
  const listHasNext = ref(false)
  const isLoadingList = ref(false)
  const isLoadingMore = ref(false)
  let listPage = 0
  let listParams: SongSearchParams | undefined

  const hasListSongs = computed(() => listSongs.value.length > 0)

  // 검색/필터 변경 시: 초기화 후 첫 페이지 로드
  const fetchFirstPage = async (params?: SongSearchParams) => {
    listParams = params
    listPage = 0
    isLoadingList.value = true
    errorMessage.value = ''
    try {
      const data = await getSongsPage(params, 0, PAGE_SIZE)
      listSongs.value = data.content
      listTotal.value = data.totalElements
      listHasNext.value = data.hasNext
    } catch (error) {
      errorMessage.value = getErrorMessage(error, '곡 목록을 불러오지 못했습니다.')
    } finally {
      isLoadingList.value = false
    }
  }

  // 스크롤 하단 도달 시: 다음 페이지 추가 로드
  const fetchNextPage = async () => {
    if (!listHasNext.value || isLoadingMore.value || isLoadingList.value) return
    isLoadingMore.value = true
    try {
      const data = await getSongsPage(listParams, listPage + 1, PAGE_SIZE)
      listSongs.value.push(...data.content)
      listPage += 1
      listTotal.value = data.totalElements
      listHasNext.value = data.hasNext
    } catch (error) {
      errorMessage.value = getErrorMessage(error, '곡 목록을 불러오지 못했습니다.')
    } finally {
      isLoadingMore.value = false
    }
  }

  const fetchSongs = async (params?: SongSearchParams) => {
    isLoading.value = true
    errorMessage.value = ''

    try {
      songs.value = await getSongs(params)
      songsLoaded.value = true
    } catch (error) {
      errorMessage.value = getErrorMessage(error, '곡 목록을 불러오지 못했습니다.')
    } finally {
      isLoading.value = false
    }
  }

  // 곡 선택 모달용 전체 목록을 최초 1회만 로드 (콘티 화면 진입 시가 아니라 모달 열 때 호출)
  const songsLoaded = ref(false)
  const ensureSongsLoaded = async () => {
    if (songsLoaded.value || isLoading.value) return
    await fetchSongs()
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
    ensureSongsLoaded,
    fetchSong,
    // 무한 스크롤 목록
    listSongs,
    listTotal,
    listHasNext,
    isLoadingList,
    isLoadingMore,
    hasListSongs,
    fetchFirstPage,
    fetchNextPage,
  }
})
