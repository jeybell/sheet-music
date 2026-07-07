<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, ChevronRight, X } from '@lucide/vue'
import { getSetlist } from '../apis/setlistApi'
import { getSong } from '../apis/songApi'
import { getActiveBaseUrl } from '../apis/apiBase'

const props = defineProps<{ setlistId: number }>()
const router = useRouter()

interface PresentFile { songFileId: number; originalFileName: string | null; contentType: string | null }
interface PresentSong {
  title: string
  artist: string | null
  sheetKey: string | null
  versionName: string | null
  memo: string | null
  files: PresentFile[]
}

const songs = ref<PresentSong[]>([])
const isLoading = ref(true)
const errorMessage = ref('')
const currentIndex = ref(0)
const currentSong = computed(() => songs.value[currentIndex.value])

const fileUrl = (fileId: number, mode: 'view' | 'download' = 'view') => {
  return `${getActiveBaseUrl()}/api/song-files/${fileId}/${mode}`
}
const isPdf = (f: PresentFile) =>
  f.contentType?.includes('pdf') || f.originalFileName?.toLowerCase().endsWith('.pdf') || false
const imageFiles = computed(() => currentSong.value?.files.filter(f => !isPdf(f)) ?? [])
const pdfFiles = computed(() => currentSong.value?.files.filter(f => isPdf(f)) ?? [])

const prev = () => { if (currentIndex.value > 0) currentIndex.value-- }
const next = () => { if (currentIndex.value < songs.value.length - 1) currentIndex.value++ }

const exitPresent = () => router.push(`/setlists/${props.setlistId}`)

const handleKey = (e: KeyboardEvent) => {
  if (e.key === 'ArrowLeft') prev()
  else if (e.key === 'ArrowRight') next()
  else if (e.key === 'Escape') exitPresent()
}

// ── 터치 스와이프
const SWIPE_THRESHOLD_PX = 50
let touchStartX = 0
let touchStartY = 0
const onTouchStart = (e: TouchEvent) => {
  touchStartX = e.changedTouches[0].clientX
  touchStartY = e.changedTouches[0].clientY
}
const onTouchEnd = (e: TouchEvent) => {
  const dx = e.changedTouches[0].clientX - touchStartX
  const dy = e.changedTouches[0].clientY - touchStartY
  if (Math.abs(dx) < SWIPE_THRESHOLD_PX || Math.abs(dx) < Math.abs(dy)) return
  if (dx < 0) next()
  else prev()
}

// ── 화면 꺼짐 방지 (WakeLock API, 미지원 브라우저는 조용히 무시)
let wakeLock: WakeLockSentinel | null = null
const requestWakeLock = async () => {
  try {
    wakeLock = await navigator.wakeLock?.request('screen')
  } catch {
    wakeLock = null
  }
}
const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') void requestWakeLock()
}

const load = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const setlist = await getSetlist(props.setlistId)
    const items = [...setlist.items].sort((a, b) => a.orderNo - b.orderNo)
    const songMap = new Map<number, Awaited<ReturnType<typeof getSong>>>()
    const songIds = [...new Set(items.map(i => i.songId))]
    await Promise.all(songIds.map(async (id) => songMap.set(id, await getSong(id))))

    songs.value = items.map((item) => {
      const song = songMap.get(item.songId)
      const sheets = song?.sheets ?? song?.songSheets ?? []
      const sheet = item.songSheetId ? sheets.find(s => s.songSheetId === item.songSheetId) : sheets[0]
      return {
        title: item.songTitle,
        artist: item.songArtist,
        sheetKey: item.sheetKey,
        versionName: item.versionName,
        memo: item.memo,
        files: (sheet?.files ?? []).map(f => ({
          songFileId: f.songFileId,
          originalFileName: f.originalFileName ?? null,
          contentType: f.contentType ?? null,
        })),
      }
    })
  } catch {
    errorMessage.value = '콘티를 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void load()
  void requestWakeLock()
  document.addEventListener('keydown', handleKey)
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKey)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  void wakeLock?.release()
})
</script>

<template>
  <div class="fixed inset-0 z-50 bg-black text-white flex flex-col">
    <!-- 상단바 -->
    <div class="flex items-center justify-between px-4 py-3 bg-black/60 flex-shrink-0">
      <span class="text-sm text-zinc-400">{{ songs.length > 0 ? `${currentIndex + 1} / ${songs.length}` : '' }}</span>
      <button type="button" class="p-2 rounded-md hover:bg-zinc-800 transition-colors" @click="exitPresent">
        <X class="w-5 h-5" />
      </button>
    </div>

    <p v-if="isLoading" class="flex-1 flex items-center justify-center text-zinc-400 text-sm">불러오는 중...</p>
    <p v-else-if="errorMessage" class="flex-1 flex items-center justify-center text-destructive text-sm">{{ errorMessage }}</p>

    <template v-else>
      <!-- 현재 곡 정보 (큰 글씨) -->
      <div class="text-center px-4 pt-2 pb-4 flex-shrink-0">
        <h1 class="text-3xl sm:text-5xl font-bold leading-tight">{{ currentSong?.title }}</h1>
        <p v-if="currentSong?.artist" class="text-zinc-400 text-base sm:text-lg mt-1">{{ currentSong.artist }}</p>
        <p v-if="currentSong?.sheetKey || currentSong?.versionName" class="mt-2">
          <span class="inline-flex items-center h-8 px-3 rounded-full bg-primary/20 text-primary text-lg sm:text-xl font-semibold">
            {{ [currentSong?.sheetKey, currentSong?.versionName].filter(Boolean).join(' · ') }}
          </span>
        </p>
        <p v-if="currentSong?.memo" class="text-zinc-300 text-sm mt-2 whitespace-pre-line">{{ currentSong.memo }}</p>
      </div>

      <!-- 악보 이미지 -->
      <div
        class="flex-1 flex items-center justify-center overflow-hidden relative px-12"
        @touchstart="onTouchStart"
        @touchend="onTouchEnd"
      >
        <button
          type="button"
          :disabled="currentIndex === 0"
          class="absolute left-2 p-3 rounded-full bg-black/40 hover:bg-black/70 disabled:opacity-20 transition-colors z-10"
          @click="prev"
        >
          <ChevronLeft class="w-7 h-7" />
        </button>

        <div class="flex flex-col items-center gap-3 w-full h-full overflow-y-auto py-2">
          <template v-if="imageFiles.length > 0">
            <img
              v-for="file in imageFiles"
              :key="file.songFileId"
              :src="fileUrl(file.songFileId)"
              :alt="file.originalFileName ?? '악보'"
              class="max-w-full max-h-[70vh] object-contain rounded shadow-lg"
            />
          </template>
          <div v-for="file in pdfFiles" :key="file.songFileId" class="text-sm">
            <a
              :href="fileUrl(file.songFileId, 'download')"
              target="_blank"
              rel="noopener noreferrer"
              class="underline text-violet-300 hover:text-violet-200"
            >
              {{ file.originalFileName ?? 'PDF 파일' }} (새 탭에서 열기)
            </a>
          </div>
          <p v-if="imageFiles.length === 0 && pdfFiles.length === 0" class="text-zinc-400 text-sm">
            등록된 악보 파일이 없습니다.
          </p>
        </div>

        <button
          type="button"
          :disabled="currentIndex === songs.length - 1"
          class="absolute right-2 p-3 rounded-full bg-black/40 hover:bg-black/70 disabled:opacity-20 transition-colors z-10"
          @click="next"
        >
          <ChevronRight class="w-7 h-7" />
        </button>
      </div>

      <!-- 하단 곡 목록 -->
      <div class="flex gap-2 px-4 py-3 overflow-x-auto bg-black/60 flex-shrink-0">
        <button
          v-for="(song, i) in songs"
          :key="i"
          type="button"
          class="flex-shrink-0 px-3 py-1.5 rounded-md text-xs transition-colors"
          :class="i === currentIndex ? 'bg-violet-600 text-white' : 'bg-zinc-700 text-zinc-300 hover:bg-zinc-600'"
          @click="currentIndex = i"
        >
          {{ i + 1 }}. {{ song.title }}
        </button>
      </div>
    </template>
  </div>
</template>
