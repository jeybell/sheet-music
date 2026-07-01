<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, X, Download, Pencil, Eraser, Undo2, Trash2 } from '@lucide/vue'
import { jsPDF } from 'jspdf'
import { getSongFileAnnotation, saveSongFileAnnotation } from '../apis/songFileApi'
import type { AnnotationStroke } from '../apis/songFileApi'

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

// ── 악보 필기(스타일러스 지원, 벡터 저장) ──────────────────
const PEN_COLORS = ['#ef4444', '#3b82f6', '#22c55e', '#facc15']

const penMode = ref(false)
const penColor = ref(PEN_COLORS[0])
const penWidth = ref(3)
const isEraser = ref(false)
const strokesByFile = reactive<Record<number, AnnotationStroke[]>>({})
const loadedFileIds = new Set<number>()
const canvasEls = new Map<number, HTMLCanvasElement>()

const setCanvasRef = (fileId: number, el: Element | null) => {
  if (el) canvasEls.set(fileId, el as HTMLCanvasElement)
  else canvasEls.delete(fileId)
}

const syncCanvasSize = (fileId: number) => {
  const canvas = canvasEls.get(fileId)
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) return
  canvas.width = rect.width
  canvas.height = rect.height
  redraw(fileId)
}

const drawStroke = (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement, stroke: AnnotationStroke) => {
  const pts = stroke.points
  if (pts.length === 0) return
  ctx.globalCompositeOperation = stroke.eraser ? 'destination-out' : 'source-over'
  ctx.strokeStyle = stroke.color
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  if (pts.length === 1) {
    const x = pts[0].x * canvas.width
    const y = pts[0].y * canvas.height
    ctx.lineWidth = stroke.width * (0.5 + pts[0].pressure)
    ctx.beginPath()
    ctx.moveTo(x, y)
    ctx.lineTo(x + 0.01, y)
    ctx.stroke()
    return
  }
  for (let i = 1; i < pts.length; i++) {
    const p0 = pts[i - 1]
    const p1 = pts[i]
    ctx.lineWidth = stroke.width * (0.5 + p1.pressure)
    ctx.beginPath()
    ctx.moveTo(p0.x * canvas.width, p0.y * canvas.height)
    ctx.lineTo(p1.x * canvas.width, p1.y * canvas.height)
    ctx.stroke()
  }
}

const redraw = (fileId: number) => {
  const canvas = canvasEls.get(fileId)
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  for (const stroke of strokesByFile[fileId] ?? []) {
    drawStroke(ctx, canvas, stroke)
  }
}

const ensureAnnotationLoaded = async (fileId: number) => {
  if (loadedFileIds.has(fileId)) return
  loadedFileIds.add(fileId)
  try {
    const res = await getSongFileAnnotation(fileId)
    strokesByFile[fileId] = res.strokes ?? []
  } catch {
    strokesByFile[fileId] = []
  }
  await nextTick()
  syncCanvasSize(fileId)
}

const loadAnnotationsForCurrentSong = async () => {
  for (const file of imageFiles.value) {
    await ensureAnnotationLoaded(file.songFileId)
  }
}

watch(currentSong, () => { void loadAnnotationsForCurrentSong() })

let activeStroke: AnnotationStroke | null = null
let activeFileId: number | null = null
let activePointerId: number | null = null
let saveTimer: ReturnType<typeof setTimeout> | undefined
const lastActiveFileId = ref<number | null>(null)

const onCanvasPointerDown = (e: PointerEvent, fileId: number) => {
  if (!penMode.value) return
  const canvas = canvasEls.get(fileId)
  if (!canvas) return
  canvas.setPointerCapture(e.pointerId)
  activePointerId = e.pointerId
  activeFileId = fileId
  lastActiveFileId.value = fileId
  const rect = canvas.getBoundingClientRect()
  activeStroke = {
    points: [{ x: (e.clientX - rect.left) / rect.width, y: (e.clientY - rect.top) / rect.height, pressure: e.pressure || 0.5 }],
    color: penColor.value,
    width: penWidth.value,
    eraser: isEraser.value,
  }
  strokesByFile[fileId] = [...(strokesByFile[fileId] ?? []), activeStroke]
}

const onCanvasPointerMove = (e: PointerEvent, fileId: number) => {
  if (!activeStroke || activePointerId !== e.pointerId || activeFileId !== fileId) return
  const canvas = canvasEls.get(fileId)
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  activeStroke.points.push({
    x: (e.clientX - rect.left) / rect.width,
    y: (e.clientY - rect.top) / rect.height,
    pressure: e.pressure || 0.5,
  })
  redraw(fileId)
}

const scheduleSave = (fileId: number) => {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    void saveSongFileAnnotation(fileId, strokesByFile[fileId] ?? [])
  }, 400)
}

const onCanvasPointerUp = (e: PointerEvent, fileId: number) => {
  if (activePointerId !== e.pointerId || activeFileId !== fileId) return
  activeStroke = null
  activePointerId = null
  activeFileId = null
  scheduleSave(fileId)
}

const undoStroke = (fileId: number) => {
  const strokes = strokesByFile[fileId] ?? []
  if (strokes.length === 0) return
  strokesByFile[fileId] = strokes.slice(0, -1)
  redraw(fileId)
  scheduleSave(fileId)
}

const clearStrokes = (fileId: number) => {
  if (!confirm('이 악보의 필기를 모두 지울까요?')) return
  strokesByFile[fileId] = []
  redraw(fileId)
  scheduleSave(fileId)
}

const onWindowResize = () => {
  for (const fileId of canvasEls.keys()) syncCanvasSize(fileId)
}

onMounted(() => {
  window.addEventListener('resize', onWindowResize)
  void loadAnnotationsForCurrentSong()
})
onUnmounted(() => window.removeEventListener('resize', onWindowResize))
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
          v-if="imageFiles.length > 0"
          type="button"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs transition-colors"
          :class="penMode ? 'bg-primary text-primary-foreground' : 'bg-zinc-700 hover:bg-zinc-600'"
          @click="penMode = !penMode"
        >
          <Pencil class="w-3.5 h-3.5" />
          필기{{ penMode ? ' 종료' : '' }}
        </button>
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

    <!-- 필기 도구 모음 -->
    <div v-if="penMode" class="flex items-center gap-3 px-4 py-2 bg-black/60 flex-shrink-0 flex-wrap">
      <div class="flex items-center gap-1.5">
        <button
          v-for="c in PEN_COLORS"
          :key="c"
          type="button"
          class="w-5 h-5 rounded-full border-2 transition-transform"
          :class="!isEraser && penColor === c ? 'scale-110 border-white' : 'border-transparent'"
          :style="{ backgroundColor: c }"
          @click="isEraser = false; penColor = c"
        />
      </div>
      <input v-model.number="penWidth" type="range" min="1" max="12" class="w-20" />
      <button
        type="button"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs transition-colors"
        :class="isEraser ? 'bg-primary text-primary-foreground' : 'bg-zinc-700 hover:bg-zinc-600 text-white'"
        @click="isEraser = !isEraser"
      >
        <Eraser class="w-3.5 h-3.5" />
        지우개
      </button>
      <button
        v-if="imageFiles.length > 0"
        type="button"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs bg-zinc-700 hover:bg-zinc-600 text-white transition-colors"
        @click="undoStroke(lastActiveFileId ?? imageFiles[0].songFileId)"
      >
        <Undo2 class="w-3.5 h-3.5" />
        실행취소
      </button>
      <button
        v-if="imageFiles.length > 0"
        type="button"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs bg-zinc-700 hover:bg-zinc-600 text-white transition-colors"
        @click="clearStrokes(lastActiveFileId ?? imageFiles[0].songFileId)"
      >
        <Trash2 class="w-3.5 h-3.5" />
        전체 지우기
      </button>
    </div>

    <!-- 본문 -->
    <div class="flex-1 flex items-center justify-center overflow-hidden relative px-12">
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
          <div
            v-for="file in imageFiles"
            :key="file.songFileId"
            class="relative max-w-full max-h-[80vh]"
          >
            <img
              :src="fileUrl(file.songFileId)"
              :alt="file.originalFileName ?? '악보'"
              class="max-w-full max-h-[80vh] object-contain rounded shadow-lg block"
              @error="(e) => ((e.target as HTMLImageElement).style.display = 'none')"
              @load="() => { void ensureAnnotationLoaded(file.songFileId); syncCanvasSize(file.songFileId) }"
            />
            <canvas
              :ref="(el) => setCanvasRef(file.songFileId, el as Element | null)"
              class="absolute inset-0 w-full h-full rounded"
              :class="penMode ? 'touch-none' : 'pointer-events-none'"
              @pointerdown="onCanvasPointerDown($event, file.songFileId)"
              @pointermove="onCanvasPointerMove($event, file.songFileId)"
              @pointerup="onCanvasPointerUp($event, file.songFileId)"
              @pointercancel="onCanvasPointerUp($event, file.songFileId)"
            />
          </div>
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
