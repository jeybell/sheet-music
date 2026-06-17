<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import {
  ChevronLeft, ChevronRight, Pencil, Trash2, Plus, Upload, FileText,
  X, Eye, Download, Settings2, Music, Maximize2, ScanText,
} from '@lucide/vue'
import { deleteSong, updateSong } from '../apis/songApi'
import { deleteSongFile, uploadSongSheetFile } from '../apis/songFileApi'
import { createSongSheet, deleteSongSheet, updateSongSheet } from '../apis/songSheetApi'
import type { OcrResult } from '../types/song'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import Badge from '../components/ui/Badge.vue'
import Card from '../components/ui/Card.vue'
import { useSongStore } from '../stores/songStore'
import type { SongSheetSummary, SongFile } from '../types/song'

interface ApiErrorResponse {
  message?: string
}

const props = defineProps<{ songId: number }>()
const router = useRouter()
const songStore = useSongStore()

const song = computed(() => songStore.selectedSong)
const sheets = computed(() => song.value?.sheets ?? song.value?.songSheets ?? [])

const apiError = (error: unknown, fallback: string) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return fallback
}

const toOpt = (v: string) => v.trim() || null

const fileUrl = (fileId: number, mode: 'view' | 'download' = 'view') => {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/api/song-files/${fileId}/${mode}`
}

const isPdf = (file: { contentType?: string | null; originalFileName?: string | null }) => {
  if (file.contentType?.includes('pdf')) return true
  return file.originalFileName?.toLowerCase().endsWith('.pdf') ?? false
}

// ── 슬라이드(이미지 1장 = 1슬라이드) ─────────────────────
interface Slide {
  sheet: SongSheetSummary
  file: SongFile
}

const slides = computed<Slide[]>(() =>
  sheets.value.flatMap((sheet) =>
    (sheet.files ?? []).map((file) => ({ sheet, file })),
  ),
)

const currentIndex = ref(0)
const currentSlide = computed<Slide | undefined>(() => slides.value[currentIndex.value])

watch(slides, (list) => {
  if (currentIndex.value > list.length - 1) currentIndex.value = Math.max(0, list.length - 1)
})

const go = (delta: number) => {
  const len = slides.value.length
  if (len === 0) return
  currentIndex.value = (currentIndex.value + delta + len) % len
}
const goTo = (i: number) => { currentIndex.value = i }

const slideLabel = (slide: Slide) => slide.sheet.versionName || slide.sheet.sheetKey || '버전'

const onKey = (e: KeyboardEvent) => {
  if (e.key === 'ArrowLeft') go(-1)
  else if (e.key === 'ArrowRight') go(1)
}

// ── 관리 패널 토글
const showManage = ref(false)

// ── 곡 수정
const isEditing = ref(false)
const editForm = reactive({ title: '', artist: '', composer: '', memo: '' })
const editError = ref('')
const isSavingEdit = ref(false)

const startEdit = () => {
  editForm.title = song.value?.title ?? ''
  editForm.artist = song.value?.artist ?? ''
  editForm.composer = song.value?.composer ?? ''
  editForm.memo = song.value?.memo ?? ''
  editError.value = ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editError.value = ''
}

const handleUpdateSong = async () => {
  if (!editForm.title.trim()) {
    editError.value = '제목은 필수입니다.'
    return
  }
  isSavingEdit.value = true
  editError.value = ''
  try {
    await updateSong(props.songId, {
      title: editForm.title.trim(),
      artist: toOpt(editForm.artist),
      composer: toOpt(editForm.composer),
      memo: toOpt(editForm.memo),
    })
    await songStore.fetchSong(props.songId)
    isEditing.value = false
  } catch (e) {
    editError.value = apiError(e, '수정에 실패했습니다.')
  } finally {
    isSavingEdit.value = false
  }
}

// ── 곡 삭제
const handleDeleteSong = async () => {
  if (!confirm(`"${song.value?.title}" 곡을 삭제할까요?`)) return
  try {
    await deleteSong(props.songId)
    await router.push('/songs')
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 악보 버전 추가
const showAddSheet = ref(false)
const sheetForm = reactive({ sheetKey: '', versionName: '', memo: '' })
const sheetError = ref('')
const isCreatingSheet = ref(false)

const resetSheetForm = () => {
  sheetForm.sheetKey = ''
  sheetForm.versionName = ''
  sheetForm.memo = ''
  sheetError.value = ''
}

const handleCreateSheet = async () => {
  sheetError.value = ''
  isCreatingSheet.value = true
  try {
    await createSongSheet(props.songId, {
      sheetKey: toOpt(sheetForm.sheetKey),
      versionName: toOpt(sheetForm.versionName),
      memo: toOpt(sheetForm.memo),
    })
    resetSheetForm()
    showAddSheet.value = false
    await songStore.fetchSong(props.songId)
  } catch (e) {
    sheetError.value = apiError(e, '악보 버전 추가에 실패했습니다.')
  } finally {
    isCreatingSheet.value = false
  }
}

// ── 악보 버전 삭제
const handleDeleteSheet = async (sheet: SongSheetSummary) => {
  const label = sheet.versionName || sheet.sheetKey || '이 버전'
  if (!confirm(`"${label}"을 삭제할까요?`)) return
  try {
    await deleteSongSheet(sheet.songSheetId)
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 파일 업로드
const selectedFiles = ref<Record<number, File | undefined>>({})
const uploadInputKeys = ref<Record<number, number>>({})
const uploadMessages = ref<Record<number, string | undefined>>({})
const uploadErrors = ref<Record<number, string | undefined>>({})
const uploadingSheets = ref<Record<number, boolean>>({})
const ocrResults = reactive<Record<number, OcrResult | undefined>>({})

const handleFileChange = (event: Event, sheetId: number) => {
  const input = event.target as HTMLInputElement
  selectedFiles.value[sheetId] = input.files?.[0]
  uploadMessages.value[sheetId] = undefined
  uploadErrors.value[sheetId] = undefined
}

const handleUpload = async (sheetId: number) => {
  const file = selectedFiles.value[sheetId]
  if (!file) {
    uploadMessages.value[sheetId] = '파일을 선택해주세요.'
    return
  }
  uploadingSheets.value[sheetId] = true
  uploadErrors.value[sheetId] = undefined
  uploadMessages.value[sheetId] = undefined
  try {
    const result = await uploadSongSheetFile(sheetId, file)
    selectedFiles.value[sheetId] = undefined
    uploadInputKeys.value[sheetId] = (uploadInputKeys.value[sheetId] ?? 0) + 1
    uploadMessages.value[sheetId] = '업로드 완료'
    if (result.ocrResult && (result.ocrResult.title || result.ocrResult.key || result.ocrResult.chords?.length)) {
      ocrResults[sheetId] = result.ocrResult
    }
    await songStore.fetchSong(props.songId)
  } catch (e) {
    uploadErrors.value[sheetId] = apiError(e, '업로드에 실패했습니다.')
  } finally {
    uploadingSheets.value[sheetId] = false
  }
}

const dismissOcr = (sheetId: number) => {
  delete ocrResults[sheetId]
}

const applyOcrKey = async (sheet: SongSheetSummary, key: string) => {
  try {
    await updateSongSheet(sheet.songSheetId, {
      sheetKey: key,
      versionName: sheet.versionName,
      memo: sheet.memo,
    })
    delete ocrResults[sheet.songSheetId]
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '키 적용에 실패했습니다.'))
  }
}

const applyOcrTitle = async (title: string) => {
  if (!song.value) return
  try {
    await updateSong(props.songId, {
      title,
      artist: song.value.artist ?? null,
      composer: song.value.composer ?? null,
      memo: song.value.memo ?? null,
    })
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '제목 적용에 실패했습니다.'))
  }
}

// ── 파일 삭제
const handleDeleteFile = async (fileId: number, fileName: string) => {
  if (!confirm(`"${fileName}" 파일을 삭제할까요?`)) return
  try {
    await deleteSongFile(fileId)
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '파일 삭제에 실패했습니다.'))
  }
}

const loadSong = () => {
  if (Number.isFinite(props.songId)) void songStore.fetchSong(props.songId)
}

onMounted(() => {
  loadSong()
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
watch(() => props.songId, loadSong)
</script>

<template>
  <DefaultLayout>
    <div class="mb-5">
      <button
        type="button"
        class="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
        @click="$router.push('/songs')"
      >
        <ChevronLeft class="w-4 h-4" />
        목록으로
      </button>
    </div>

    <p v-if="songStore.isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="songStore.errorMessage" class="text-sm text-destructive">{{ songStore.errorMessage }}</p>

    <template v-else-if="song">
      <div class="grid grid-cols-1 lg:grid-cols-[1fr_18rem] gap-6">
        <!-- ── 악보 뷰어 (메인) ───────────────────────── -->
        <div>
          <div
            class="relative rounded-xl border border-border bg-muted/40 overflow-hidden flex items-center justify-center"
            style="min-height: 60vh"
          >
            <template v-if="currentSlide">
              <!-- 이미지 슬라이드 -->
              <img
                v-if="!isPdf(currentSlide.file)"
                :src="fileUrl(currentSlide.file.songFileId)"
                :alt="currentSlide.file.originalFileName ?? '악보'"
                class="max-h-[78vh] w-full object-contain"
              />
              <!-- PDF 슬라이드 -->
              <div v-else class="flex flex-col items-center gap-3 py-20 text-center px-6">
                <FileText class="w-10 h-10 text-muted-foreground" />
                <p class="text-sm text-muted-foreground">
                  {{ currentSlide.file.originalFileName ?? 'PDF 파일' }}
                </p>
                <a
                  :href="fileUrl(currentSlide.file.songFileId, 'view')"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center gap-1.5 h-9 px-4 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:opacity-90"
                >
                  <Eye class="w-4 h-4" />
                  PDF 새 탭에서 보기
                </a>
              </div>

              <!-- 좌우 화살표 -->
              <template v-if="slides.length > 1">
                <button
                  type="button"
                  aria-label="이전 악보"
                  class="absolute left-2 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-background/80 backdrop-blur border border-border flex items-center justify-center text-foreground hover:bg-background transition-colors"
                  @click="go(-1)"
                >
                  <ChevronLeft class="w-5 h-5" />
                </button>
                <button
                  type="button"
                  aria-label="다음 악보"
                  class="absolute right-2 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-background/80 backdrop-blur border border-border flex items-center justify-center text-foreground hover:bg-background transition-colors"
                  @click="go(1)"
                >
                  <ChevronRight class="w-5 h-5" />
                </button>
              </template>

              <!-- 상단 캡션: 키/버전 + 카운터 -->
              <div class="absolute top-2 left-2 flex items-center gap-2">
                <span class="inline-flex items-center gap-1.5 h-7 px-2.5 rounded-full bg-background/80 backdrop-blur border border-border text-xs font-medium text-foreground">
                  <Badge v-if="currentSlide.sheet.sheetKey" variant="violet">{{ currentSlide.sheet.sheetKey }}</Badge>
                  {{ currentSlide.sheet.versionName || (currentSlide.sheet.sheetKey ? '' : '버전') }}
                </span>
              </div>
              <div class="absolute top-2 right-2 flex items-center gap-1.5">
                <a
                  :href="fileUrl(currentSlide.file.songFileId)"
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="전체화면"
                  class="w-7 h-7 rounded-full bg-background/80 backdrop-blur border border-border flex items-center justify-center text-foreground hover:bg-background"
                >
                  <Maximize2 class="w-3.5 h-3.5" />
                </a>
                <span class="h-7 px-2.5 rounded-full bg-background/80 backdrop-blur border border-border text-xs font-medium text-muted-foreground flex items-center">
                  {{ currentIndex + 1 }} / {{ slides.length }}
                </span>
              </div>
            </template>

            <!-- 악보 없음 -->
            <div v-else class="flex flex-col items-center gap-3 py-20 text-center px-6">
              <Music class="w-10 h-10 text-muted-foreground" />
              <p class="text-sm font-medium text-foreground">등록된 악보가 없습니다</p>
              <p class="text-sm text-muted-foreground">아래 ‘악보 관리’에서 버전과 파일을 추가하세요.</p>
            </div>
          </div>

          <!-- 썸네일 인디케이터 -->
          <div v-if="slides.length > 1" class="mt-3 flex flex-wrap gap-2">
            <button
              v-for="(slide, i) in slides"
              :key="`${slide.file.songFileId}`"
              type="button"
              class="h-8 px-3 rounded-md border text-xs font-medium transition-colors"
              :class="i === currentIndex
                ? 'border-primary bg-primary-soft text-primary'
                : 'border-border bg-card text-muted-foreground hover:bg-muted'"
              @click="goTo(i)"
            >
              {{ slideLabel(slide) }}
            </button>
          </div>
        </div>

        <!-- ── 곡 정보 사이드 ─────────────────────────── -->
        <aside class="lg:order-last">
          <Card class="p-4">
            <h1 class="text-lg font-bold text-foreground leading-snug break-words">{{ song.title }}</h1>
            <dl class="mt-3 space-y-1.5 text-sm">
              <div v-if="song.artist" class="flex gap-2">
                <dt class="text-muted-foreground shrink-0 w-12">아티스트</dt>
                <dd class="text-foreground break-words">{{ song.artist }}</dd>
              </div>
              <div v-if="song.composer" class="flex gap-2">
                <dt class="text-muted-foreground shrink-0 w-12">작곡가</dt>
                <dd class="text-foreground break-words">{{ song.composer }}</dd>
              </div>
            </dl>
            <p v-if="song.memo" class="mt-3 text-xs text-muted-foreground whitespace-pre-line border-t border-border pt-3">
              {{ song.memo }}
            </p>

            <button
              type="button"
              class="mt-4 w-full inline-flex items-center justify-center gap-1.5 h-9 px-4 rounded-md border border-border text-sm font-medium text-foreground hover:bg-muted transition-colors"
              @click="showManage = !showManage"
            >
              <Settings2 class="w-4 h-4" />
              {{ showManage ? '관리 닫기' : '악보 관리' }}
            </button>
          </Card>

          <!-- 버전 요약 -->
          <div v-if="sheets.length" class="mt-3 text-xs text-muted-foreground px-1">
            악보 버전 {{ sheets.length }}개 · 총 {{ slides.length }}장
          </div>
        </aside>
      </div>

      <!-- ── 관리 패널 (접이식) ─────────────────────── -->
      <div v-if="showManage" class="mt-8 border-t border-border pt-6">
        <!-- 곡 정보 수정 -->
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-base font-semibold text-foreground">곡 관리</h2>
          <div v-if="!isEditing" class="flex gap-2">
            <Button variant="outline" size="sm" @click="startEdit">
              <Pencil class="w-3.5 h-3.5" />
              곡 정보 수정
            </Button>
            <Button variant="destructive" size="sm" @click="handleDeleteSong">
              <Trash2 class="w-3.5 h-3.5" />
              곡 삭제
            </Button>
          </div>
        </div>

        <Card v-if="isEditing" class="p-5 mb-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-semibold text-foreground">곡 정보 수정</h3>
            <button type="button" class="text-muted-foreground hover:text-foreground" @click="cancelEdit">
              <X class="w-4 h-4" />
            </button>
          </div>
          <p v-if="editError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4">{{ editError }}</p>
          <div class="flex flex-col gap-4">
            <div class="flex flex-col gap-1.5">
              <Label for="edit-title">제목 <span class="text-destructive">*</span></Label>
              <Input id="edit-title" v-model="editForm.title" type="text" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div class="flex flex-col gap-1.5">
                <Label for="edit-artist">아티스트</Label>
                <Input id="edit-artist" v-model="editForm.artist" type="text" />
              </div>
              <div class="flex flex-col gap-1.5">
                <Label for="edit-composer">작곡가</Label>
                <Input id="edit-composer" v-model="editForm.composer" type="text" />
              </div>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label for="edit-memo">메모</Label>
              <Textarea id="edit-memo" v-model="editForm.memo" rows="3" />
            </div>
            <div class="flex gap-2">
              <Button :disabled="isSavingEdit" @click="handleUpdateSong">
                {{ isSavingEdit ? '저장 중...' : '저장' }}
              </Button>
              <Button variant="outline" :disabled="isSavingEdit" @click="cancelEdit">취소</Button>
            </div>
          </div>
        </Card>

        <!-- 악보 버전 관리 -->
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-foreground">악보 버전</h3>
          <Button variant="outline" size="sm" @click="showAddSheet = !showAddSheet">
            <template v-if="showAddSheet"><X class="w-3.5 h-3.5" />취소</template>
            <template v-else><Plus class="w-3.5 h-3.5" />버전 추가</template>
          </Button>
        </div>

        <Card v-if="showAddSheet" class="p-5 mb-4 bg-muted/40">
          <p v-if="sheetError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4">{{ sheetError }}</p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
            <div class="flex flex-col gap-1.5">
              <Label for="sheet-key">키</Label>
              <Input id="sheet-key" v-model="sheetForm.sheetKey" type="text" placeholder="예) C, G, Am" />
            </div>
            <div class="flex flex-col gap-1.5">
              <Label for="version-name">버전명</Label>
              <Input id="version-name" v-model="sheetForm.versionName" type="text" placeholder="예) 원본, 남성용" />
            </div>
          </div>
          <div class="flex flex-col gap-1.5 mb-4">
            <Label for="sheet-memo">메모</Label>
            <Textarea id="sheet-memo" v-model="sheetForm.memo" rows="2" />
          </div>
          <Button :disabled="isCreatingSheet" @click="handleCreateSheet">
            {{ isCreatingSheet ? '추가 중...' : '추가' }}
          </Button>
        </Card>

        <p v-if="sheets.length === 0" class="text-sm text-muted-foreground py-6 text-center">
          등록된 악보 버전이 없습니다.
        </p>

        <div v-else class="flex flex-col gap-3">
          <Card v-for="sheet in sheets" :key="sheet.songSheetId" class="p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2 flex-wrap">
                <Badge v-if="sheet.sheetKey" variant="violet">{{ sheet.sheetKey }}</Badge>
                <span v-if="sheet.versionName" class="text-sm font-medium text-foreground">{{ sheet.versionName }}</span>
                <span v-if="!sheet.sheetKey && !sheet.versionName" class="text-sm text-muted-foreground">버전명 없음</span>
              </div>
              <Button variant="destructive" size="sm" @click="handleDeleteSheet(sheet)">
                <Trash2 class="w-3.5 h-3.5" />
                삭제
              </Button>
            </div>

            <p v-if="sheet.memo" class="text-xs text-muted-foreground mb-3">{{ sheet.memo }}</p>

            <!-- 파일 목록 -->
            <div v-if="sheet.files?.length" class="flex flex-col gap-2 mb-3">
              <div
                v-for="file in sheet.files"
                :key="file.songFileId"
                class="flex items-center justify-between py-1.5 px-3 rounded-md bg-muted/50"
              >
                <div class="flex items-center gap-2 min-w-0">
                  <FileText class="w-3.5 h-3.5 text-muted-foreground shrink-0" />
                  <span class="text-xs text-foreground truncate">
                    {{ file.originalFileName ?? file.storedFileName ?? '파일명 없음' }}
                  </span>
                </div>
                <div class="flex gap-1.5 shrink-0 ml-2">
                  <a
                    :href="fileUrl(file.songFileId, 'view')"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="p-1 text-muted-foreground hover:text-foreground transition-colors"
                    aria-label="보기"
                  >
                    <Eye class="w-3.5 h-3.5" />
                  </a>
                  <a
                    :href="fileUrl(file.songFileId, 'download')"
                    :download="file.originalFileName ?? 'sheet'"
                    class="p-1 text-muted-foreground hover:text-foreground transition-colors"
                    aria-label="다운로드"
                  >
                    <Download class="w-3.5 h-3.5" />
                  </a>
                  <button
                    type="button"
                    class="p-1 text-muted-foreground hover:text-destructive transition-colors"
                    aria-label="삭제"
                    @click="handleDeleteFile(file.songFileId, file.originalFileName ?? '파일')"
                  >
                    <X class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
            <p v-else class="text-xs text-muted-foreground mb-3">파일 없음</p>

            <!-- 파일 업로드 -->
            <div class="flex items-center gap-2 flex-wrap">
              <label
                class="flex items-center gap-1.5 h-8 px-3 rounded-md border border-border bg-card text-xs text-foreground hover:bg-muted cursor-pointer transition-colors"
              >
                <Upload class="w-3.5 h-3.5" />
                파일 선택
                <input
                  :key="`${sheet.songSheetId}-${uploadInputKeys[sheet.songSheetId] ?? 0}`"
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg"
                  class="hidden"
                  @change="handleFileChange($event, sheet.songSheetId)"
                />
              </label>
              <span v-if="selectedFiles[sheet.songSheetId]" class="text-xs text-muted-foreground truncate max-w-[160px]">
                {{ selectedFiles[sheet.songSheetId]?.name }}
              </span>
              <Button
                size="sm"
                :disabled="uploadingSheets[sheet.songSheetId] || !selectedFiles[sheet.songSheetId]"
                @click="handleUpload(sheet.songSheetId)"
              >
                {{ uploadingSheets[sheet.songSheetId] ? '업로드 중...' : '업로드' }}
              </Button>
              <span v-if="uploadMessages[sheet.songSheetId]" class="text-xs text-green-500">
                {{ uploadMessages[sheet.songSheetId] }}
              </span>
              <span v-if="uploadErrors[sheet.songSheetId]" class="text-xs text-destructive">
                {{ uploadErrors[sheet.songSheetId] }}
              </span>
            </div>

            <!-- OCR 결과 배너 -->
            <div
              v-if="ocrResults[sheet.songSheetId]"
              class="mt-3 p-3 rounded-md bg-muted border border-border text-sm"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                  <ScanText class="w-3.5 h-3.5 text-primary" />
                  OCR 감지 결과
                </span>
                <button
                  type="button"
                  class="text-muted-foreground hover:text-foreground transition-colors"
                  @click="dismissOcr(sheet.songSheetId)"
                >
                  <X class="w-3.5 h-3.5" />
                </button>
              </div>
              <div class="flex flex-col gap-1.5">
                <div v-if="ocrResults[sheet.songSheetId]?.title" class="flex items-center gap-2 flex-wrap">
                  <span class="text-xs text-muted-foreground w-8">제목</span>
                  <span class="text-xs text-foreground">{{ ocrResults[sheet.songSheetId]?.title }}</span>
                  <button
                    type="button"
                    class="text-xs text-primary hover:underline"
                    @click="applyOcrTitle(ocrResults[sheet.songSheetId]!.title!)"
                  >
                    곡 제목에 적용
                  </button>
                </div>
                <div v-if="ocrResults[sheet.songSheetId]?.key" class="flex items-center gap-2 flex-wrap">
                  <span class="text-xs text-muted-foreground w-8">키</span>
                  <Badge variant="violet">{{ ocrResults[sheet.songSheetId]?.key }}</Badge>
                  <button
                    type="button"
                    class="text-xs text-primary hover:underline"
                    @click="applyOcrKey(sheet, ocrResults[sheet.songSheetId]!.key!)"
                  >
                    악보 키에 적용
                  </button>
                </div>
                <div v-if="ocrResults[sheet.songSheetId]?.chords?.length" class="flex items-center gap-2 flex-wrap">
                  <span class="text-xs text-muted-foreground w-8">코드</span>
                  <span class="text-xs text-foreground">{{ ocrResults[sheet.songSheetId]?.chords?.join(', ') }}</span>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </template>
  </DefaultLayout>
</template>
