<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, Upload, X, ScanText, CheckCircle, AlertCircle, Loader2 } from '@lucide/vue'
import { createSong } from '../apis/songApi'
import { createSongSheet } from '../apis/songSheetApi'
import { uploadSongSheetFile } from '../apis/songFileApi'
import { previewOcr } from '../apis/ocrApi'
import { extractApiError } from '../composables/useApiError'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'

type ItemStatus = 'ocr-loading' | 'ready' | 'saving' | 'done' | 'error'

interface BulkItem {
  id: string
  file: File
  previewUrl: string
  status: ItemStatus
  title: string
  sheetKey: string
  artist: string
  errorMessage: string
  songId: number | null
}

const router = useRouter()
const items = ref<BulkItem[]>([])
const isSavingAll = ref(false)
const isDragging = ref(false)

const pendingCount = computed(() => items.value.filter(i => i.status === 'ready').length)
const doneCount = computed(() => items.value.filter(i => i.status === 'done').length)
const ocrLoadingCount = computed(() => items.value.filter(i => i.status === 'ocr-loading').length)
const canSaveAll = computed(() => pendingCount.value > 0 && !isSavingAll.value)

const addFiles = (files: FileList | File[]) => {
  const allowed = ['image/png', 'image/jpeg', 'image/jpg']
  Array.from(files).forEach(file => {
    if (!allowed.includes(file.type)) return
    const item: BulkItem = {
      id: crypto.randomUUID(),
      file,
      previewUrl: URL.createObjectURL(file),
      status: 'ocr-loading',
      title: '',
      sheetKey: '',
      artist: '',
      errorMessage: '',
      songId: null,
    }
    items.value.push(item)
    runOcr(item)
  })
}

const runOcr = async (item: BulkItem) => {
  try {
    const result = await previewOcr(item.file)
    const target = items.value.find(i => i.id === item.id)
    if (!target) return
    if (result.title) target.title = result.title
    if (result.key) target.sheetKey = result.key
    if (result.artist) target.artist = result.artist
    target.status = 'ready'
  } catch {
    const target = items.value.find(i => i.id === item.id)
    if (target) target.status = 'ready'
  }
}

const handleFileInput = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files) addFiles(input.files)
  input.value = ''
}

const handleDrop = (e: DragEvent) => {
  isDragging.value = false
  if (e.dataTransfer?.files) addFiles(e.dataTransfer.files)
}

const removeItem = (id: string) => {
  const idx = items.value.findIndex(i => i.id === id)
  if (idx !== -1) {
    URL.revokeObjectURL(items.value[idx].previewUrl)
    items.value.splice(idx, 1)
  }
}

const saveItem = async (item: BulkItem) => {
  const title = item.title.trim()
  if (!title) {
    item.errorMessage = '제목을 입력하세요.'
    return
  }
  item.status = 'saving'
  item.errorMessage = ''
  try {
    const song = await createSong({
      title,
      artist: item.artist.trim() || null,
    })
    const sheet = await createSongSheet(song.songId, {
      sheetKey: item.sheetKey.trim() || null,
      versionName: null,
      memo: null,
    })
    await uploadSongSheetFile(sheet.songSheetId, item.file)
    item.songId = song.songId
    item.status = 'done'
  } catch (error) {
    item.errorMessage = extractApiError(error, '저장 실패')
    item.status = 'error'
  }
}

const saveAll = async () => {
  isSavingAll.value = true
  const readyItems = items.value.filter(i => i.status === 'ready' || i.status === 'error')
  await Promise.allSettled(readyItems.map(saveItem))
  isSavingAll.value = false
}

const statusLabel: Record<ItemStatus, string> = {
  'ocr-loading': 'OCR 분석 중',
  'ready': '대기',
  'saving': '저장 중',
  'done': '완료',
  'error': '오류',
}
</script>

<template>
  <DefaultLayout>
    <div class="mb-6">
      <button
        type="button"
        class="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
        @click="$router.push('/songs')"
      >
        <ChevronLeft class="w-4 h-4" />
        목록으로
      </button>
    </div>

    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold text-foreground">일괄 업로드</h1>
      <div class="flex items-center gap-3">
        <span v-if="items.length > 0" class="text-sm text-muted-foreground">
          {{ doneCount }}/{{ items.length }}개 완료
          <span v-if="ocrLoadingCount > 0" class="ml-1">(OCR {{ ocrLoadingCount }}개 분석 중)</span>
        </span>
        <Button :disabled="!canSaveAll" @click="saveAll">
          {{ isSavingAll ? '저장 중...' : `전체 저장 (${pendingCount}개)` }}
        </Button>
      </div>
    </div>

    <!-- 드롭존 -->
    <div
      class="relative mb-6 flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed transition-colors cursor-pointer"
      :class="isDragging ? 'border-primary bg-primary/5' : 'border-border bg-muted/20 hover:bg-muted/40'"
      style="min-height: 140px"
      @dragover.prevent="isDragging = true"
      @dragleave="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <label class="absolute inset-0 cursor-pointer" aria-label="파일 선택">
        <input
          type="file"
          accept=".png,.jpg,.jpeg"
          multiple
          class="hidden"
          @change="handleFileInput"
        />
      </label>
      <Upload class="w-7 h-7 text-muted-foreground" />
      <p class="text-sm text-muted-foreground font-medium">이미지를 드래그하거나 클릭해서 선택</p>
      <p class="text-xs text-muted-foreground">PNG, JPG, JPEG · 여러 파일 동시 선택 가능</p>
    </div>

    <!-- 아이템 그리드 -->
    <div v-if="items.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="item in items"
        :key="item.id"
        class="relative bg-card rounded-xl border shadow-sm overflow-hidden flex flex-col"
        :class="{
          'border-border': item.status === 'ready' || item.status === 'ocr-loading',
          'border-primary/40': item.status === 'saving',
          'border-green-500/40': item.status === 'done',
          'border-destructive/40': item.status === 'error',
        }"
      >
        <!-- 이미지 미리보기 -->
        <div class="relative bg-muted/30" style="height: 160px">
          <img
            :src="item.previewUrl"
            :alt="item.file.name"
            class="w-full h-full object-contain"
          />

          <!-- 상태 배지 -->
          <div
            class="absolute top-2 left-2 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
            :class="{
              'bg-muted text-muted-foreground': item.status === 'ocr-loading',
              'bg-background/80 text-foreground': item.status === 'ready',
              'bg-primary/10 text-primary': item.status === 'saving',
              'bg-green-500/10 text-green-600 dark:text-green-400': item.status === 'done',
              'bg-destructive/10 text-destructive': item.status === 'error',
            }"
          >
            <Loader2 v-if="item.status === 'ocr-loading' || item.status === 'saving'" class="w-3 h-3 animate-spin" />
            <CheckCircle v-else-if="item.status === 'done'" class="w-3 h-3" />
            <AlertCircle v-else-if="item.status === 'error'" class="w-3 h-3" />
            <ScanText v-else-if="item.status === 'ready'" class="w-3 h-3" />
            {{ statusLabel[item.status] }}
          </div>

          <!-- 삭제 버튼 -->
          <button
            v-if="item.status !== 'saving'"
            type="button"
            class="absolute top-2 right-2 w-6 h-6 rounded-full bg-background/80 backdrop-blur border border-border flex items-center justify-center text-foreground hover:bg-background transition-colors"
            @click="removeItem(item.id)"
          >
            <X class="w-3 h-3" />
          </button>
        </div>

        <!-- 입력 영역 -->
        <div class="p-3 flex flex-col gap-2 flex-1">
          <p v-if="item.errorMessage" class="text-xs text-destructive">{{ item.errorMessage }}</p>

          <div class="flex flex-col gap-1">
            <Label class="text-xs">제목 *</Label>
            <Input
              v-model="item.title"
              type="text"
              placeholder="곡 제목"
              class="h-8 text-xs"
              :disabled="item.status === 'saving' || item.status === 'done'"
            />
          </div>

          <div class="flex gap-2">
            <div class="flex flex-col gap-1 flex-1">
              <Label class="text-xs">키</Label>
              <Input
                v-model="item.sheetKey"
                type="text"
                placeholder="C, G..."
                class="h-8 text-xs"
                :disabled="item.status === 'saving' || item.status === 'done'"
              />
            </div>
            <div class="flex flex-col gap-1 flex-1">
              <Label class="text-xs">아티스트</Label>
              <Input
                v-model="item.artist"
                type="text"
                placeholder="아티스트"
                class="h-8 text-xs"
                :disabled="item.status === 'saving' || item.status === 'done'"
              />
            </div>
          </div>

          <!-- done 상태: 상세 이동 -->
          <div v-if="item.status === 'done'" class="mt-auto pt-1">
            <button
              type="button"
              class="text-xs text-primary hover:underline"
              @click="router.push(`/songs/${item.songId}`)"
            >
              상세 보기 →
            </button>
          </div>

          <!-- error 상태: 재저장 -->
          <div v-else-if="item.status === 'error'" class="mt-auto pt-1">
            <button
              type="button"
              class="text-xs text-primary hover:underline"
              @click="saveItem(item)"
            >
              다시 저장
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="py-12 flex flex-col items-center text-center text-muted-foreground">
      <Upload class="w-10 h-10 mb-3 opacity-30" />
      <p class="text-sm">이미지를 업로드하면 여기에 표시됩니다.</p>
    </div>
  </DefaultLayout>
</template>
