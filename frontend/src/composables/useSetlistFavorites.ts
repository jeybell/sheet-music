import { ref } from 'vue'

const FAVORITES_KEY = 'setlist-favorites'
const RECENTS_KEY = 'setlist-recents'
const MAX_RECENTS = 5

const readIds = (key: string): number[] => {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const persist = (key: string, ids: number[]) => {
  localStorage.setItem(key, JSON.stringify(ids))
}

// 브라우저(로컬 저장소) 단위 즐겨찾기/최근 열어본 콘티. 별도 사용자별 서버 저장은 하지 않는다.
const favoriteIds = ref<number[]>(readIds(FAVORITES_KEY))
const recentIds = ref<number[]>(readIds(RECENTS_KEY))

export function useSetlistFavorites() {
  const isFavorite = (setlistId: number) => favoriteIds.value.includes(setlistId)

  const toggleFavorite = (setlistId: number) => {
    favoriteIds.value = isFavorite(setlistId)
      ? favoriteIds.value.filter((id) => id !== setlistId)
      : [...favoriteIds.value, setlistId]
    persist(FAVORITES_KEY, favoriteIds.value)
  }

  const addRecent = (setlistId: number) => {
    recentIds.value = [setlistId, ...recentIds.value.filter((id) => id !== setlistId)].slice(0, MAX_RECENTS)
    persist(RECENTS_KEY, recentIds.value)
  }

  return { favoriteIds, recentIds, isFavorite, toggleFavorite, addRecent }
}
