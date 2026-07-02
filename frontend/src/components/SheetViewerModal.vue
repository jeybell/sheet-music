<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ChevronLeft, ChevronRight, X, Download } from '@lucide/vue'
import { jsPDF } from 'jspdf'

export interface ViewerSong {
  title: string
  artist: string | null
  sheetKey: string | null
  versionName: string | null
  files: { songFileId: number; originalFileName: string | null; contentType: string | null }[]
  sheetDeleted?: boolean
}

const props = defineProps<{
  songs: ViewerSong[]
  setlistTitle: string | null
}>()

const emit = defineEmits<{ close: [] }>()

const currentIndex = ref(0)
const currentSong = computed(() => props.songs[currentIndex.value])
const isDownloading = ref(false)

const prev = () => { if (currentIndex.value > 0) currentIndex.value-- }
const next = () => { if (currentIndex.value < props.songs.length - 1) currentIndex.value++ }

const fileUrl = (fileId: number, mode: 'view' | 'download' = 'view') => {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/api/song-files/${fileId}/${mode}`
}

const isPdf = (file: { contentType: string | null; originalFileName: string | null }) => {
  if (file.contentType?.includes('pdf')) return true
  return file.originalFileName?.toLowerCase().endsWith('.pdf') ?? false
}

const imageFiles = computed(() =>
  currentSong.value?.files.filter(f => !isPdf(f)) ?? []
)
const pdfFiles = computed(() =>
  currentSong.value?.files.filter(f => isPdf(f)) ?? []
)

const handleKey = (e: KeyboardEvent) => {
  if (e.key === 'ArrowLeft') prev()
  else if (e.key === 'ArrowRight') next()
  else if (e.key === 'Escape') emit('close')
}

onMounted(() => document.addEventListener('keydown', handleKey))
onUnmounted(() => document.removeEventListener('keydown', handleKey))

// ── 모바일 터치 스와이프 (좌우로 이전/다음 슬라이드 이동)
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

const downloadPdf = async () => {
  isDownloading.value = true
  try {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
    let firstPage = true

    for (const song of props.songs) {
      const imgs = song.files.filter(f => !isPdf(f))
      for (const file of imgs) {
        const url = fileUrl(file.songFileId)
        const blob = await fetch(url).then(r => r.blob())
        const dataUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader()
          reader.onload = () => resolve(reader.result as string)
          reader.readAsDataURL(blob)
        })
        const img = new Image()
        await new Promise<void>((resolve) => {
          img.onload = () => resolve()
          img.src = dataUrl
        })
        if (!firstPage) doc.addPage()
        firstPage = false
        const pageW = doc.internal.pageSize.getWidth()
        const pageH = doc.internal.pageSize.getHeight()
        const ratio = Math.min(pageW / img.width, pageH / img.height)
        const w = img.width * ratio
        const h = img.height * ratio
        const x = (pageW - w) / 2
        const y = (pageH - h) / 2
        doc.addImage(dataUrl, 'JPEG', x, y, w, h)
      }
    }

    const filename = (props.setlistTitle ?? 'setlist') + '.pdf'
    doc.save(filename)
  } finally {
    isDownloading.value = false
  }
}
</script>

<template>
  <div
    class="fixed inset-0 z-50 bg-black/90 flex flex-col"
    @click.self="emit('close')"
  >
    <!-- 헤더 -->
    <div class="flex items-center justify-between px-4 py-3 text-white bg-black/60 flex-shrink-0">
      <div class="flex items-center gap-3 min-w-0">
        <span class="text-sm text-zinc-400">{{ currentIndex + 1 }} / {{ songs.length }}</span>
        <div class="min-w-0">
          <span class="font-semibold truncate">{{ currentSong?.title }}</span>
          <span v-if="currentSong?.sheetKey || currentSong?.versionName" class="ml-2 text-xs text-zinc-400">
            {{ [currentSong.sheetKey, currentSong.versionName].filter(Boolean).join(' · ') }}
          </span>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        <button
          type="button"
          :disabled="isDownloading"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs bg-zinc-700 hover:bg-zinc-600 disabled:opacity-50 transition-colors"
          @click="downloadPdf"
        >
          <Download class="w-3.5 h-3.5" />
          {{ isDownloading ? 'PDF 생성 중...' : 'PDF 다운로드' }}
        </button>
        <button
          type="button"
          class="p-1.5 rounded-md hover:bg-zinc-700 transition-colors"
          @click="emit('close')"
        >
          <X class="w-5 h-5" />
        </button>
      </div>
    </div>

    <!-- 본문 -->
    <div
      class="flex-1 flex items-center justify-center overflow-hidden relative px-12"
      @touchstart="onTouchStart"
      @touchend="onTouchEnd"
    >
      <!-- 이전 -->
      <button
        type="button"
        :disabled="currentIndex === 0"
        class="absolute left-2 p-2 rounded-full bg-black/40 hover:bg-black/70 text-white disabled:opacity-20 transition-colors"
        @click="prev"
      >
        <ChevronLeft class="w-6 h-6" />
      </button>

      <!-- 콘텐츠 -->
      <div class="flex flex-col items-center gap-3 w-full h-full overflow-y-auto py-4">
        <!-- 삭제된 악보 버전 참조 안내 -->
        <div
          v-if="currentSong?.sheetDeleted"
          class="flex flex-col items-center gap-2 text-center px-6"
        >
          <div class="w-12 h-12 rounded-full bg-yellow-500/20 flex items-center justify-center">
            <span class="text-yellow-400 text-xl">!</span>
          </div>
          <p class="text-yellow-300 text-sm font-medium">악보 버전이 삭제되었습니다</p>
          <p class="text-zinc-400 text-xs">이 곡에 연결된 악보 버전이 삭제되어 표시할 수 없습니다.<br>셋리스트에서 악보 버전을 다시 선택해주세요.</p>
        </div>

        <template v-else-if="imageFiles.length > 0">
          <img
            v-for="file in imageFiles"
            :key="file.songFileId"
            :src="fileUrl(file.songFileId)"
            :alt="file.originalFileName ?? '악보'"
            class="max-w-full max-h-[80vh] object-contain rounded shadow-lg"
            @error="(e) => ((e.target as HTMLImageElement).style.display = 'none')"
          />
        </template>
        <div
          v-for="file in pdfFiles"
          v-show="!currentSong?.sheetDeleted"
          :key="file.songFileId"
          class="text-white text-sm"
        >
          <a
            :href="fileUrl(file.songFileId, 'download')"
            target="_blank"
            rel="noopener noreferrer"
            class="underline text-violet-300 hover:text-violet-200"
          >
            {{ file.originalFileName ?? 'PDF 파일' }} (새 탭에서 열기)
          </a>
        </div>
        <p
          v-if="!currentSong?.sheetDeleted && imageFiles.length === 0 && pdfFiles.length === 0"
          class="text-zinc-400 text-sm"
        >
          등록된 악보 파일이 없습니다.
        </p>
      </div>

      <!-- 다음 -->
      <button
        type="button"
        :disabled="currentIndex === songs.length - 1"
        class="absolute right-2 p-2 rounded-full bg-black/40 hover:bg-black/70 text-white disabled:opacity-20 transition-colors"
        @click="next"
      >
        <ChevronRight class="w-6 h-6" />
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
  </div>
</template>
