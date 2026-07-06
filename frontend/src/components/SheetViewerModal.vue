<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, X, Download, Pencil, Eraser, Undo2, Trash2, SunMedium, Wand2 } from '@lucide/vue'
import { jsPDF } from 'jspdf'
import { getSongFileAnnotation, saveSongFileAnnotation, replaceSongFileContent } from '../apis/songFileApi'
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
  initialIndex?: number
}>()

const emit = defineEmits<{ close: [] }>()

const currentIndex = ref(
  props.initialIndex != null && props.initialIndex >= 0 && props.initialIndex < props.songs.length
    ? props.initialIndex
    : 0,
)
const currentSong = computed(() => props.songs[currentIndex.value])
const isDownloading = ref(false)

const prev = () => { if (currentIndex.value > 0) currentIndex.value-- }
const next = () => { if (currentIndex.value < props.songs.length - 1) currentIndex.value++ }

// 밝기 보정 저장 후 같은 URL이 브라우저에 캐시되어 옛 이미지가 보이는 걸 막기 위한 버전 쿼리.
const fileVersions = reactive<Record<number, number>>({})

const fileUrl = (fileId: number, mode: 'view' | 'download' = 'view') => {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  const url = `${base}/api/song-files/${fileId}/${mode}`
  const version = fileVersions[fileId]
  return version ? `${url}?v=${version}` : url
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
  else if (e.key === 'Escape') handleClose()
}

onMounted(() => document.addEventListener('keydown', handleKey))
onUnmounted(() => document.removeEventListener('keydown', handleKey))

// ── 모바일 터치 스와이프 (좌우로 이전/다음 슬라이드 이동)
const SWIPE_THRESHOLD_PX = 50
let touchStartX = 0
let touchStartY = 0

const onTouchStart = (e: TouchEvent) => {
  if (penMode.value) return // 필기 모드에서는 캔버스 드로잉과 스와이프 내비게이션이 충돌하므로 스와이프 비활성화
  touchStartX = e.changedTouches[0].clientX
  touchStartY = e.changedTouches[0].clientY
}

const onTouchEnd = (e: TouchEvent) => {
  if (penMode.value) return
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

watch(currentSong, () => {
  void flushSaves() // 곡 전환 시 이전 곡의 미저장 필기 저장
  void loadAnnotationsForCurrentSong()
  closeBrightnessEditor() // 곡을 넘기면 편집 중이던 밝기 보정 오버레이는 닫는다
})

let activeStroke: AnnotationStroke | null = null
let activeFileId: number | null = null
let activePointerId: number | null = null
// 파일별로 별도 디바운스 타이머를 둔다. 공유 타이머 하나만 쓰면 여러 악보를 빠르게
// 오가며 그릴 때 이전 파일의 예약된 저장이 취소되어 필기가 유실될 수 있다.
// 획마다 저장하지 않고, 변경된 파일을 dirty 로 표시만 해둔다.
// 실제 저장(flush)은 필기 종료(펜 off)·모달 닫기·곡 전환·언마운트 시 1회씩 수행한다 (#147).
const dirtyFiles = new Set<number>()
const lastActiveFileId = ref<number | null>(null)

const markDirty = (fileId: number) => { dirtyFiles.add(fileId) }

const flushSaves = async () => {
  if (dirtyFiles.size === 0) return
  const ids = [...dirtyFiles]
  dirtyFiles.clear()
  await Promise.all(
    ids.map((id) =>
      saveSongFileAnnotation(id, strokesByFile[id] ?? []).catch(() => {
        dirtyFiles.add(id) // 실패 시 재시도 대상으로 다시 표시
      }),
    ),
  )
}

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

const onCanvasPointerUp = (e: PointerEvent, fileId: number) => {
  if (activePointerId !== e.pointerId || activeFileId !== fileId) return
  activeStroke = null
  activePointerId = null
  activeFileId = null
  markDirty(fileId)
}

const undoStroke = (fileId: number) => {
  const strokes = strokesByFile[fileId] ?? []
  if (strokes.length === 0) return
  strokesByFile[fileId] = strokes.slice(0, -1)
  redraw(fileId)
  markDirty(fileId)
}

const clearStrokes = (fileId: number) => {
  if (!confirm('이 악보의 필기를 모두 지울까요?')) return
  strokesByFile[fileId] = []
  redraw(fileId)
  markDirty(fileId)
}

// 필기 종료(펜 off) 시 1회 저장
watch(penMode, (on, was) => {
  if (was && !on) void flushSaves()
})

// 모달 닫기 전에 미저장 필기 저장
const handleClose = () => {
  void flushSaves()
  emit('close')
}

const onWindowResize = () => {
  for (const fileId of canvasEls.keys()) syncCanvasSize(fileId)
}

// ── 악보 이미지 밝기·대비 보정 ──────────────────────────────
interface BrightnessTarget {
  songFileId: number
  originalFileName: string | null
  contentType: string | null
}
const editingFile = ref<BrightnessTarget | null>(null)
const editingImage = ref<HTMLImageElement | null>(null)
const editingImageUrl = ref<string | null>(null)
const isLoadingEditImage = ref(false)
const brightness = ref(0) // -100..100
const contrast = ref(0) // -100..100
const isSavingBrightness = ref(false)
const brightnessError = ref('')

const cssFilter = computed(() => `brightness(${1 + brightness.value / 100}) contrast(${1 + contrast.value / 100})`)

const revokeEditingImageUrl = () => {
  if (editingImageUrl.value) {
    URL.revokeObjectURL(editingImageUrl.value)
    editingImageUrl.value = null
  }
}

const openBrightnessEditor = async (file: BrightnessTarget) => {
  brightnessError.value = ''
  brightness.value = 0
  contrast.value = 0
  editingFile.value = file
  editingImage.value = null
  isLoadingEditImage.value = true
  try {
    // fetch 로 blob 을 받아 Image 를 만들면 <canvas>에서 픽셀을 읽어도(getImageData/toBlob)
    // cross-origin taint 가 나지 않는다(다른 도메인의 <img> 를 바로 그리면 오류가 날 수 있음).
    const blob = await fetch(fileUrl(file.songFileId)).then((r) => r.blob())
    revokeEditingImageUrl()
    const url = URL.createObjectURL(blob)
    editingImageUrl.value = url
    const img = new Image()
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('이미지를 불러오지 못했습니다.'))
      img.src = url
    })
    editingImage.value = img
  } catch {
    brightnessError.value = '이미지를 불러오지 못했습니다.'
  } finally {
    isLoadingEditImage.value = false
  }
}

const closeBrightnessEditor = () => {
  editingFile.value = null
  editingImage.value = null
  revokeEditingImageUrl()
}

/**
 * 악보는 대부분 흰 배경 위에 어두운 음표/글자가 놓인 구도라, 평균 밝기가 아니라
 * "배경(가장 밝은 축에 속하는 화소들)"의 밝기를 기준으로 보정해야 배경이 확실히
 * 흰색에 가까워진다. 샘플 화소를 밝기순으로 정렬해 상위 10% 지점(배경으로 추정)의
 * 값이 거의 흰색(250)이 되도록 배율을 계산한다.
 */
const applyAutoLevel = () => {
  const img = editingImage.value
  if (!img) return
  const sampleSize = 64
  const canvas = document.createElement('canvas')
  canvas.width = sampleSize
  canvas.height = sampleSize
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.drawImage(img, 0, 0, sampleSize, sampleSize)
  const { data } = ctx.getImageData(0, 0, sampleSize, sampleSize)
  const luminances: number[] = []
  for (let i = 0; i < data.length; i += 4) {
    luminances.push(0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2])
  }
  luminances.sort((a, b) => a - b)
  const backgroundLuminance = Math.max(luminances[Math.floor(luminances.length * 0.9)], 1)
  const target = 250
  const brightnessPercent = Math.round(((target - backgroundLuminance) / backgroundLuminance) * 100)
  brightness.value = Math.min(Math.max(brightnessPercent, -50), 400)
  contrast.value = 25
}

const saveBrightnessEdit = async () => {
  const file = editingFile.value
  const img = editingImage.value
  if (!file || !img || isSavingBrightness.value) return
  isSavingBrightness.value = true
  brightnessError.value = ''
  try {
    const canvas = document.createElement('canvas')
    canvas.width = img.naturalWidth
    canvas.height = img.naturalHeight
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('canvas unsupported')
    // 투명 배경 PNG가 어두운 뷰어 배경에 비쳐 어둡게 보이는 문제를 막기 위해
    // 흰 배경을 먼저 채운 뒤 그 위에 이미지를 그린다(알파를 흰색으로 플래튼).
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.filter = cssFilter.value
    ctx.drawImage(img, 0, 0)

    const mimeType = file.contentType?.startsWith('image/') ? file.contentType : 'image/jpeg'
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, mimeType, 0.92))
    if (!blob) throw new Error('이미지 생성에 실패했습니다.')

    await replaceSongFileContent(file.songFileId, blob, file.originalFileName ?? 'sheet')
    fileVersions[file.songFileId] = (fileVersions[file.songFileId] ?? 0) + 1
    closeBrightnessEditor()
  } catch {
    brightnessError.value = '저장에 실패했습니다. 다시 시도해주세요.'
  } finally {
    isSavingBrightness.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', onWindowResize)
  void loadAnnotationsForCurrentSong()
})
onUnmounted(() => {
  window.removeEventListener('resize', onWindowResize)
  void flushSaves() // 라우트 이동 등으로 언마운트될 때 미저장 필기 저장
  revokeEditingImageUrl()
})
</script>

<template>
  <div
    class="fixed inset-0 z-50 bg-black/90 flex flex-col"
    @click.self="handleClose"
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
          @click="handleClose"
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
          <div
            v-for="file in imageFiles"
            :key="file.songFileId"
            class="relative max-w-full max-h-[80vh]"
          >
            <img
              :src="fileUrl(file.songFileId)"
              :alt="file.originalFileName ?? '악보'"
              class="max-w-full max-h-[80vh] object-contain rounded shadow-lg block bg-white"
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
            <!-- 밝기 보정 진입 버튼 -->
            <button
              v-if="!penMode"
              type="button"
              class="absolute top-2 right-2 flex items-center gap-1 px-2 py-1 rounded-md text-xs bg-black/60 hover:bg-black/80 text-white transition-colors"
              @click="openBrightnessEditor(file)"
            >
              <SunMedium class="w-3.5 h-3.5" />
              밝기 보정
            </button>
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

    <!-- 밝기·대비 보정 오버레이 -->
    <div
      v-if="editingFile"
      class="fixed inset-0 z-[60] bg-black/95 flex flex-col"
      @click.self="closeBrightnessEditor"
    >
      <div class="flex items-center justify-between px-4 py-3 text-white bg-black/60 flex-shrink-0">
        <span class="font-semibold text-sm">밝기·대비 보정</span>
        <button type="button" class="p-1.5 rounded-md hover:bg-zinc-700 transition-colors" @click="closeBrightnessEditor">
          <X class="w-5 h-5" />
        </button>
      </div>

      <div class="flex-1 flex items-center justify-center overflow-hidden px-6 py-4">
        <p v-if="isLoadingEditImage" class="text-zinc-400 text-sm">불러오는 중...</p>
        <p v-else-if="brightnessError && !editingImageUrl" class="text-red-400 text-sm">{{ brightnessError }}</p>
        <img
          v-else-if="editingImageUrl"
          :src="editingImageUrl"
          alt="편집 중인 악보"
          class="max-w-full max-h-full object-contain rounded shadow-lg bg-white"
          :style="{ filter: cssFilter }"
        />
      </div>

      <div class="flex flex-col gap-3 px-4 py-3 bg-black/60 flex-shrink-0">
        <p v-if="brightnessError && editingImageUrl" class="text-xs text-red-400">{{ brightnessError }}</p>
        <div class="flex items-center gap-3">
          <span class="text-xs text-zinc-300 w-12 shrink-0">밝기</span>
          <input v-model.number="brightness" type="range" min="-100" max="400" class="flex-1" />
          <span class="text-xs text-zinc-400 w-10 text-right shrink-0">{{ brightness }}</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-xs text-zinc-300 w-12 shrink-0">대비</span>
          <input v-model.number="contrast" type="range" min="-100" max="100" class="flex-1" />
          <span class="text-xs text-zinc-400 w-10 text-right shrink-0">{{ contrast }}</span>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <button
            type="button"
            :disabled="!editingImage"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs bg-zinc-700 hover:bg-zinc-600 disabled:opacity-50 transition-colors"
            @click="applyAutoLevel"
          >
            <Wand2 class="w-3.5 h-3.5" />
            자동 보정
          </button>
          <button
            type="button"
            :disabled="!editingImage || isSavingBrightness"
            class="ml-auto flex items-center gap-1.5 px-4 py-1.5 rounded-md text-xs bg-primary text-primary-foreground disabled:opacity-50 transition-opacity hover:opacity-90"
            @click="saveBrightnessEdit"
          >
            {{ isSavingBrightness ? '저장 중...' : '저장' }}
          </button>
          <button
            type="button"
            :disabled="isSavingBrightness"
            class="px-4 py-1.5 rounded-md text-xs bg-zinc-700 hover:bg-zinc-600 disabled:opacity-50 transition-colors"
            @click="closeBrightnessEditor"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
