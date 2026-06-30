<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChevronLeft, ChevronRight, ChevronDown, Pencil, Trash2, Plus, Upload, FileText,
  X, Eye, Download, Settings2, Music, Maximize2, ScanText, AlignLeft, Type, ExternalLink,
} from '@lucide/vue'
import { extractApiError } from '../composables/useApiError'
import { deleteSong, updateSong, updateLyrics, getAllTags, addSongLink, updateSongLink, deleteSongLink } from '../apis/songApi'
import { deleteSongFile, uploadSongSheetFile } from '../apis/songFileApi'
import { createSongSheet, deleteSongSheet, updateSongSheet } from '../apis/songSheetApi'
import { runOcrOnFile } from '../apis/ocrApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import Badge from '../components/ui/Badge.vue'
import Card from '../components/ui/Card.vue'
import TagInput from '../components/ui/TagInput.vue'
import { useSongStore } from '../stores/songStore'
import type { SongSheetSummary, SongFile } from '../types/song'

const props = defineProps<{ songId: number }>()
const router = useRouter()
const songStore = useSongStore()

const song = computed(() => songStore.selectedSong)
const sheets = computed(() => song.value?.sheets ?? song.value?.songSheets ?? [])

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
const editForm = reactive({ title: '', artist: '', memo: '', youtubeUrl: '', tags: [] as string[] })
const allTags = ref<string[]>([])
const editError = ref('')
const isSavingEdit = ref(false)

const startEdit = () => {
  editForm.title = song.value?.title ?? ''
  editForm.artist = song.value?.artist ?? ''
  editForm.memo = song.value?.memo ?? ''
  editForm.youtubeUrl = song.value?.youtubeUrl ?? ''
  editForm.tags = [...(song.value?.tags ?? [])]
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
      memo: toOpt(editForm.memo),
      youtubeUrl: toOpt(editForm.youtubeUrl),
      tags: editForm.tags,
    })
    await songStore.fetchSong(props.songId)
    isEditing.value = false
  } catch (e) {
    editError.value = extractApiError(e, '수정에 실패했습니다.')
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
    alert(extractApiError(e, '삭제에 실패했습니다.'))
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
    sheetError.value = extractApiError(e, '악보 버전 추가에 실패했습니다.')
  } finally {
    isCreatingSheet.value = false
  }
}

// ── 악보 버전 수정
const editingSheetId = ref<number | null>(null)
const sheetEditForm = reactive({ sheetKey: '', versionName: '', memo: '' })
const isUpdatingSheet = ref(false)
const sheetUpdateError = ref('')

const startEditSheet = (sheet: SongSheetSummary) => {
  editingSheetId.value = sheet.songSheetId
  sheetEditForm.sheetKey = sheet.sheetKey ?? ''
  sheetEditForm.versionName = sheet.versionName ?? ''
  sheetEditForm.memo = sheet.memo ?? ''
  sheetUpdateError.value = ''
}

const cancelEditSheet = () => {
  editingSheetId.value = null
  sheetUpdateError.value = ''
}

const handleUpdateSheet = async (sheetId: number) => {
  isUpdatingSheet.value = true
  sheetUpdateError.value = ''
  try {
    await updateSongSheet(sheetId, {
      sheetKey: toOpt(sheetEditForm.sheetKey),
      versionName: toOpt(sheetEditForm.versionName),
      memo: toOpt(sheetEditForm.memo),
    })
    await songStore.fetchSong(props.songId)
    editingSheetId.value = null
  } catch (e) {
    sheetUpdateError.value = extractApiError(e, '수정에 실패했습니다.')
  } finally {
    isUpdatingSheet.value = false
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
    alert(extractApiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 파일 업로드
const selectedFiles = ref<Record<number, File | undefined>>({})
const uploadInputKeys = ref<Record<number, number>>({})
const uploadMessages = ref<Record<number, string | undefined>>({})
const uploadErrors = ref<Record<number, string | undefined>>({})
const uploadingSheets = ref<Record<number, boolean>>({})

// ── 수동 OCR ──────────────────────────────────────────────
const isOcrRunning = ref(false)
const ocrError = ref('')

const firstImageFileId = computed((): number | null => {
  for (const sheet of sheets.value) {
    for (const f of sheet.files ?? []) {
      if (f.contentType?.startsWith('image/')) return f.songFileId
    }
  }
  return null
})

const runOcrForInfo = async () => {
  const fileId = firstImageFileId.value
  if (!fileId) return
  isOcrRunning.value = true
  ocrError.value = ''
  try {
    const result = await runOcrOnFile(fileId)
    if (!result) { ocrError.value = 'OCR 결과가 없습니다.'; return }
    if (result.title) await updateSong(props.songId, { title: result.title, artist: result.artist ?? song.value?.artist ?? null, memo: song.value?.memo ?? null })
    await songStore.fetchSong(props.songId)
  } catch (e) {
    ocrError.value = extractApiError(e, 'OCR 실행에 실패했습니다.')
  } finally {
    isOcrRunning.value = false
  }
}

const runOcrForLyrics = async () => {
  const fileId = firstImageFileId.value
  if (!fileId) return
  isOcrRunning.value = true
  ocrError.value = ''
  try {
    const result = await runOcrOnFile(fileId)
    if (!result?.lyrics) { ocrError.value = 'OCR에서 가사를 추출하지 못했습니다.'; return }
    lyricsForm.value = result.lyrics
  } catch (e) {
    ocrError.value = extractApiError(e, 'OCR 실행에 실패했습니다.')
  } finally {
    isOcrRunning.value = false
  }
}

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
    await uploadSongSheetFile(sheetId, file)
    selectedFiles.value[sheetId] = undefined
    uploadInputKeys.value[sheetId] = (uploadInputKeys.value[sheetId] ?? 0) + 1
    uploadMessages.value[sheetId] = '업로드 완료'
    await songStore.fetchSong(props.songId)
  } catch (e) {
    uploadErrors.value[sheetId] = extractApiError(e, '업로드에 실패했습니다.')
  } finally {
    uploadingSheets.value[sheetId] = false
  }
}

// ── 파일 삭제
const handleDeleteFile = async (fileId: number, fileName: string) => {
  if (!confirm(`"${fileName}" 파일을 삭제할까요?`)) return
  try {
    await deleteSongFile(fileId)
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(extractApiError(e, '파일 삭제에 실패했습니다.'))
  }
}

// ── 링크 관리 ─────────────────────────────────────────────
const extractYoutubeId = (url: string): string | null => {
  try {
    const u = new URL(url)
    if (u.hostname === 'youtu.be') return u.pathname.slice(1).split('?')[0]
    if (u.hostname.includes('youtube.com')) return u.searchParams.get('v')
  } catch { /* invalid url */ }
  return null
}

type LinkPlatform = 'youtube' | 'spotify' | 'soundcloud' | 'melon' | 'bugs' | 'genie' | 'other'

const detectPlatform = (url: string): LinkPlatform => {
  try {
    const h = new URL(url).hostname
    if (h.includes('youtube.com') || h.includes('youtu.be')) return 'youtube'
    if (h.includes('spotify.com')) return 'spotify'
    if (h.includes('soundcloud.com')) return 'soundcloud'
    if (h.includes('melon.com')) return 'melon'
    if (h.includes('music.bugs.co.kr')) return 'bugs'
    if (h.includes('genie.co.kr')) return 'genie'
  } catch { /* invalid url */ }
  return 'other'
}

const platformLabel = (url: string): string => ({
  youtube: 'YouTube',
  spotify: 'Spotify',
  soundcloud: 'SoundCloud',
  melon: 'Melon',
  bugs: 'Bugs',
  genie: 'Genie',
  other: '링크',
}[detectPlatform(url)])

const platformColor = (url: string): string => ({
  youtube: 'text-red-500',
  spotify: 'text-green-500',
  soundcloud: 'text-orange-500',
  melon: 'text-emerald-500',
  bugs: 'text-purple-500',
  genie: 'text-blue-500',
  other: 'text-muted-foreground',
}[detectPlatform(url)])

const embedYoutubeId = ref<string | null>(null)

const toggleEmbed = (url: string) => {
  const id = extractYoutubeId(url)
  embedYoutubeId.value = embedYoutubeId.value === id ? null : id
}

// 링크 편집 (인라인)
const linkForm = reactive({ url: '' })
const isAddingLink = ref(false)
const editingLinkId = ref<number | null>(null)
const editLinkForm = reactive({ title: '', url: '' })
const linksExpanded = ref(false)

const startAddLink = () => { linkForm.url = ''; isAddingLink.value = true }
const cancelAddLink = () => { isAddingLink.value = false }

const handleAddLink = async () => {
  const url = linkForm.url.trim()
  if (!url) return
  try {
    await addSongLink(song.value!.songId, { title: platformLabel(url), url })
    await loadSong()
    isAddingLink.value = false
    linksExpanded.value = true
  } catch { /* ignore */ }
}

const startEditLink = (link: { linkId: number; title: string; url: string }) => {
  editingLinkId.value = link.linkId
  editLinkForm.title = link.title
  editLinkForm.url = link.url
}

const handleUpdateLink = async (linkId: number) => {
  try {
    await updateSongLink(linkId, { title: editLinkForm.title.trim(), url: editLinkForm.url.trim() })
    await loadSong()
    editingLinkId.value = null
  } catch { /* ignore */ }
}

const handleDeleteLink = async (linkId: number) => {
  if (!confirm('링크를 삭제할까요?')) return
  try {
    await deleteSongLink(linkId)
    await loadSong()
  } catch { /* ignore */ }
}

// ── 가사 관리 ─────────────────────────────────────────────
const isEditingLyrics = ref(false)
const lyricsForm = ref('')
const isSavingLyrics = ref(false)
const lyricsError = ref('')
const lyricsOpen = ref(false)
const lyricsFontSize = ref<'sm' | 'base' | 'lg'>('base')
const lyricsFontSizeClass = computed(() => ({
  sm: 'text-xs',
  base: 'text-sm',
  lg: 'text-base',
}[lyricsFontSize.value]))

const startEditLyrics = () => {
  lyricsForm.value = song.value?.lyrics ?? ''
  lyricsError.value = ''
  isEditingLyrics.value = true
}

const cancelEditLyrics = () => {
  isEditingLyrics.value = false
  lyricsError.value = ''
}

const handleSaveLyrics = async () => {
  isSavingLyrics.value = true
  lyricsError.value = ''
  try {
    await updateLyrics(props.songId, lyricsForm.value.trim() || null)
    await songStore.fetchSong(props.songId)
    isEditingLyrics.value = false
  } catch (e) {
    lyricsError.value = extractApiError(e, '가사 저장에 실패했습니다.')
  } finally {
    isSavingLyrics.value = false
  }
}

const loadSong = () => {
  if (Number.isFinite(props.songId)) void songStore.fetchSong(props.songId)
}

onMounted(async () => {
  loadSong()
  window.addEventListener('keydown', onKey)
  allTags.value = await getAllTags()
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
})
watch(() => props.songId, loadSong)
</script>

<template>
  <DefaultLayout>
    <div class="mb-3">
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
      <div class="h-[calc(100dvh-7.5rem)] flex flex-col">
      <div class="grid grid-cols-1 lg:grid-cols-[1fr_18rem] gap-6 flex-1 min-h-0 lg:overflow-hidden">
        <!-- ── 악보 뷰어 (메인) ───────────────────────── -->
        <div class="flex flex-col min-h-0">
          <div
            class="relative rounded-xl border border-border bg-muted/40 overflow-hidden flex items-center justify-center flex-1 min-h-0"
          >
            <template v-if="currentSlide">
              <!-- 이미지 슬라이드 -->
              <img
                v-if="!isPdf(currentSlide.file)"
                :src="fileUrl(currentSlide.file.songFileId)"
                :alt="currentSlide.file.originalFileName ?? '악보'"
                class="max-h-full w-full object-contain"
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
          <div v-if="slides.length > 1" class="mt-3 shrink-0 flex flex-wrap gap-2">
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
        <aside class="lg:order-last lg:overflow-y-auto lg:h-full">
          <Card class="p-4">
            <h1 class="text-lg font-bold text-foreground leading-snug break-words">{{ song.title }}</h1>
            <dl class="mt-3 space-y-1.5 text-sm">
              <div v-if="song.artist" class="flex gap-2">
                <dt class="text-muted-foreground shrink-0 whitespace-nowrap">아티스트</dt>
                <dd class="text-foreground break-words">{{ song.artist }}</dd>
              </div>
            </dl>
            <p v-if="song.memo" class="mt-3 text-xs text-muted-foreground whitespace-pre-line border-t border-border pt-3">
              {{ song.memo }}
            </p>

            <div v-if="song.tags?.length" class="mt-3 flex flex-wrap gap-1.5 border-t border-border pt-3">
              <span
                v-for="tag in song.tags"
                :key="tag"
                class="inline-flex items-center h-6 px-2 rounded-full bg-primary/15 text-primary text-xs font-medium"
              >{{ tag }}</span>
            </div>

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

          <!-- 링크 섹션 -->
          <Card class="mt-3 overflow-hidden">
            <div class="flex items-center gap-2 px-4 py-3 border-b border-border">
              <ExternalLink class="w-4 h-4 text-muted-foreground shrink-0" />
              <span class="text-sm font-semibold text-foreground flex-1">링크</span>
              <button
                type="button"
                class="inline-flex items-center gap-1 h-7 px-2.5 rounded-md border border-border text-xs font-medium text-foreground hover:bg-muted transition-colors"
                @click="startAddLink"
              >
                <Plus class="w-3.5 h-3.5" />
                추가
              </button>
            </div>

            <!-- 링크 추가 폼 -->
            <div v-if="isAddingLink" class="px-4 py-3 border-b border-border flex gap-2">
              <input
                v-model="linkForm.url"
                type="url"
                placeholder="URL (YouTube, Spotify, Melon 등)"
                class="flex-1 h-8 px-3 text-sm rounded-md border border-input bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                @keydown.enter="handleAddLink"
              />
              <button
                type="button"
                class="h-8 px-3 rounded-md bg-primary text-primary-foreground text-xs font-medium hover:opacity-90 transition-opacity shrink-0"
                @click="handleAddLink"
              >저장</button>
              <button
                type="button"
                class="h-8 px-3 rounded-md border border-border text-xs text-foreground hover:bg-muted transition-colors shrink-0"
                @click="cancelAddLink"
              >취소</button>
            </div>

            <!-- 링크 없음 -->
            <p v-if="!song?.links?.length && !isAddingLink" class="px-4 py-3 text-xs text-muted-foreground">
              등록된 링크가 없습니다.
            </p>

            <!-- 링크 목록 -->
            <div
              v-for="(link, idx) in song?.links"
              v-show="idx === 0 || linksExpanded"
              :key="link.linkId"
              class="border-b border-border last:border-0"
            >
              <!-- 편집 모드 -->
              <div v-if="editingLinkId === link.linkId" class="px-4 py-3 flex flex-col gap-2">
                <input
                  v-model="editLinkForm.title"
                  type="text"
                  placeholder="제목"
                  class="w-full h-8 px-3 text-sm rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                />
                <input
                  v-model="editLinkForm.url"
                  type="url"
                  placeholder="URL"
                  class="w-full h-8 px-3 text-sm rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                />
                <div class="flex gap-2">
                  <button
                    type="button"
                    class="h-8 px-3 rounded-md bg-primary text-primary-foreground text-xs font-medium hover:opacity-90"
                    @click="handleUpdateLink(link.linkId)"
                  >저장</button>
                  <button
                    type="button"
                    class="h-8 px-3 rounded-md border border-border text-xs text-foreground hover:bg-muted"
                    @click="editingLinkId = null"
                  >취소</button>
                </div>
              </div>

              <!-- 표시 모드 -->
              <div v-else class="flex items-center gap-2 px-4 py-2.5">
                <span :class="['shrink-0 text-xs font-medium w-16 truncate', platformColor(link.url)]">
                  {{ link.title || platformLabel(link.url) }}
                </span>
                <a
                  :href="link.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="flex-1 min-w-0 text-xs text-muted-foreground hover:text-foreground truncate transition-colors"
                >{{ link.url }}</a>
                <!-- YouTube 임베드 토글 -->
                <button
                  v-if="extractYoutubeId(link.url)"
                  type="button"
                  class="text-xs text-muted-foreground hover:text-foreground px-1.5 py-0.5 rounded hover:bg-muted transition-colors shrink-0"
                  @click="toggleEmbed(link.url)"
                >
                  {{ embedYoutubeId === extractYoutubeId(link.url) ? '닫기' : '영상' }}
                </button>
                <button
                  type="button"
                  class="p-1 text-muted-foreground hover:text-foreground shrink-0"
                  @click="startEditLink(link)"
                ><Pencil class="w-3.5 h-3.5" /></button>
                <button
                  type="button"
                  class="p-1 text-muted-foreground hover:text-destructive shrink-0"
                  @click="handleDeleteLink(link.linkId)"
                ><Trash2 class="w-3.5 h-3.5" /></button>
              </div>

              <!-- YouTube 임베드 -->
              <div v-if="embedYoutubeId && embedYoutubeId === extractYoutubeId(link.url)" class="border-t border-border">
                <iframe
                  :src="`https://www.youtube.com/embed/${embedYoutubeId}`"
                  class="w-full aspect-video"
                  frameborder="0"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowfullscreen
                />
              </div>
            </div>

            <!-- 더보기 / 접기 -->
            <button
              v-if="(song?.links?.length ?? 0) > 1"
              type="button"
              class="w-full px-4 py-2 text-xs text-muted-foreground hover:bg-muted transition-colors flex items-center gap-1 justify-center border-t border-border"
              @click="linksExpanded = !linksExpanded"
            >
              <ChevronDown class="w-3.5 h-3.5 transition-transform" :class="{ 'rotate-180': linksExpanded }" />
              {{ linksExpanded ? '접기' : `더보기 ${(song?.links?.length ?? 0) - 1}개` }}
            </button>
          </Card>

          <!-- 가사 섹션 (아코디언) -->
          <Card class="mt-3 overflow-hidden">
            <!-- 헤더 (항상 표시) -->
            <button
              type="button"
              class="w-full flex items-center justify-between p-4 hover:bg-muted/40 transition-colors"
              @click="lyricsOpen = !lyricsOpen"
            >
              <span class="text-sm font-semibold text-foreground flex items-center gap-1.5">
                <AlignLeft class="w-4 h-4" />
                가사
                <span v-if="song?.lyrics" class="text-xs font-normal text-muted-foreground ml-1">
                  ({{ song.lyrics.length }}자)
                </span>
              </span>
              <ChevronDown
                class="w-4 h-4 text-muted-foreground transition-transform duration-200"
                :class="{ 'rotate-180': lyricsOpen }"
              />
            </button>

            <!-- 펼쳐진 내용 -->
            <div v-if="lyricsOpen" class="px-4 pb-4 border-t border-border">
              <!-- 가사 편집 폼 -->
              <div v-if="isEditingLyrics" class="flex flex-col gap-2 mt-3">
                <div v-if="firstImageFileId" class="flex items-center gap-2">
                  <button
                    type="button"
                    :disabled="isOcrRunning"
                    class="inline-flex items-center gap-1.5 h-7 px-2 rounded-md border border-border text-xs text-muted-foreground hover:bg-muted disabled:opacity-50 transition-colors"
                    @click="runOcrForLyrics"
                  >
                    <ScanText class="w-3 h-3" :class="{ 'animate-pulse text-primary': isOcrRunning }" />
                    {{ isOcrRunning ? 'OCR 분석 중...' : 'OCR로 가사 추출' }}
                  </button>
                  <span v-if="ocrError" class="text-xs text-destructive">{{ ocrError }}</span>
                </div>
                <p v-if="lyricsError" class="text-xs text-destructive">{{ lyricsError }}</p>
                <Textarea
                  v-model="lyricsForm"
                  rows="10"
                  placeholder="가사를 입력하세요"
                  class="text-sm font-mono leading-relaxed resize-y"
                />
                <div class="flex gap-2">
                  <Button size="sm" :disabled="isSavingLyrics" @click="handleSaveLyrics">
                    {{ isSavingLyrics ? '저장 중...' : '저장' }}
                  </Button>
                  <Button size="sm" variant="outline" :disabled="isSavingLyrics" @click="cancelEditLyrics">취소</Button>
                </div>
              </div>

              <!-- 가사 표시 -->
              <template v-else-if="song?.lyrics">
                <!-- 툴바: 편집 + 글자 크기 -->
                <div class="flex items-center justify-between mt-3 mb-2">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 h-7 px-2 rounded-md border border-border text-xs font-medium text-foreground hover:bg-muted transition-colors"
                    @click="startEditLyrics"
                  >
                    <Pencil class="w-3 h-3" />
                    편집
                  </button>
                  <div class="flex items-center gap-1">
                    <Type class="w-3.5 h-3.5 text-muted-foreground" />
                    <button
                      v-for="size in (['sm', 'base', 'lg'] as const)"
                      :key="size"
                      type="button"
                      class="h-6 px-1.5 rounded text-xs font-medium transition-colors"
                      :class="lyricsFontSize === size
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:bg-muted'"
                      @click="lyricsFontSize = size"
                    >{{ size === 'sm' ? 'S' : size === 'base' ? 'M' : 'L' }}</button>
                  </div>
                </div>
                <p :class="[lyricsFontSizeClass, 'whitespace-pre-line leading-relaxed text-foreground']">{{ song.lyrics }}</p>
              </template>

              <!-- 가사 없음 -->
              <div v-else class="mt-3 flex items-center justify-between">
                <p class="text-xs text-muted-foreground">등록된 가사가 없습니다.</p>
                <button
                  type="button"
                  class="inline-flex items-center gap-1 h-7 px-2 rounded-md border border-border text-xs font-medium text-foreground hover:bg-muted transition-colors"
                  @click="startEditLyrics"
                >
                  <Pencil class="w-3 h-3" />
                  입력
                </button>
              </div>
            </div>
          </Card>

          <!-- ── 관리 패널 (접이식) ─────────────────────── -->
          <div v-if="showManage" class="mt-4 border-t border-border pt-4">
            <!-- 곡 정보 수정 -->
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-base font-semibold text-foreground">곡 관리</h2>
              <div v-if="!isEditing" class="flex gap-2 flex-wrap">
                <Button
                  v-if="firstImageFileId"
                  variant="outline"
                  size="sm"
                  :disabled="isOcrRunning"
                  @click="runOcrForInfo"
                >
                  <ScanText class="w-3.5 h-3.5" :class="{ 'animate-pulse text-primary': isOcrRunning }" />
                  {{ isOcrRunning ? 'OCR 분석 중...' : 'OCR로 정보 채우기' }}
                </Button>
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
              <p v-if="editError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4 whitespace-pre-line">{{ editError }}</p>
              <div class="flex flex-col gap-4">
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-title">제목 <span class="text-destructive">*</span></Label>
                  <Input id="edit-title" v-model="editForm.title" type="text" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-artist">아티스트</Label>
                  <Input id="edit-artist" v-model="editForm.artist" type="text" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-memo">메모</Label>
                  <Textarea id="edit-memo" v-model="editForm.memo" rows="3" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label>태그</Label>
                  <TagInput v-model="editForm.tags" :suggestions="allTags" />
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
              <div class="flex gap-2 mb-3">
                <Input v-model="sheetForm.sheetKey" type="text" placeholder="키 (예: C, G)" class="flex-1" />
                <Input v-model="sheetForm.versionName" type="text" placeholder="버전명 (예: 원본)" class="flex-1" />
                <Button :disabled="isCreatingSheet" @click="handleCreateSheet" class="shrink-0">
                  {{ isCreatingSheet ? '...' : '추가' }}
                </Button>
              </div>
              <Textarea v-model="sheetForm.memo" rows="2" placeholder="메모 (선택)" class="text-xs" />
            </Card>

            <p v-if="sheets.length === 0" class="text-sm text-muted-foreground py-6 text-center">
              등록된 악보 버전이 없습니다.
            </p>

            <div v-else class="flex flex-col gap-3">
              <Card v-for="sheet in sheets" :key="sheet.songSheetId" class="p-5">
                <!-- 인라인 수정 폼 -->
                <template v-if="editingSheetId === sheet.songSheetId">
                  <div class="flex items-center justify-between mb-4">
                    <h4 class="text-sm font-semibold text-foreground">악보 버전 수정</h4>
                    <button type="button" class="text-muted-foreground hover:text-foreground" @click="cancelEditSheet">
                      <X class="w-4 h-4" />
                    </button>
                  </div>
                  <p v-if="sheetUpdateError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-3">{{ sheetUpdateError }}</p>
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
                    <div class="flex flex-col gap-1.5">
                      <Label>키</Label>
                      <Input v-model="sheetEditForm.sheetKey" type="text" placeholder="예) C, G, Am" />
                    </div>
                    <div class="flex flex-col gap-1.5">
                      <Label>버전명</Label>
                      <Input v-model="sheetEditForm.versionName" type="text" placeholder="예) 원본, 남성용" />
                    </div>
                  </div>
                  <div class="flex flex-col gap-1.5 mb-4">
                    <Label>메모</Label>
                    <Textarea v-model="sheetEditForm.memo" rows="2" />
                  </div>
                  <div class="flex gap-2">
                    <Button :disabled="isUpdatingSheet" @click="handleUpdateSheet(sheet.songSheetId)">
                      {{ isUpdatingSheet ? '저장 중...' : '저장' }}
                    </Button>
                    <Button variant="outline" :disabled="isUpdatingSheet" @click="cancelEditSheet">취소</Button>
                  </div>
                </template>

                <!-- 기본 뷰 -->
                <template v-else>
                  <div class="flex items-center justify-between mb-3">
                    <div class="flex items-center gap-2 flex-wrap">
                      <Badge v-if="sheet.sheetKey" variant="violet">{{ sheet.sheetKey }}</Badge>
                      <span v-if="sheet.versionName" class="text-sm font-medium text-foreground">{{ sheet.versionName }}</span>
                      <span v-if="!sheet.sheetKey && !sheet.versionName" class="text-sm text-muted-foreground">버전명 없음</span>
                    </div>
                    <div class="flex gap-2">
                      <Button variant="outline" size="sm" @click="startEditSheet(sheet)">
                        <Pencil class="w-3.5 h-3.5" />
                        수정
                      </Button>
                      <Button variant="destructive" size="sm" @click="handleDeleteSheet(sheet)">
                        <Trash2 class="w-3.5 h-3.5" />
                        삭제
                      </Button>
                    </div>
                  </div>
                  <p v-if="sheet.memo" class="text-xs text-muted-foreground mb-3">{{ sheet.memo }}</p>
                </template>

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
              </Card>
            </div>
          </div>
        </aside>
      </div>
      </div>
    </template>
  </DefaultLayout>
</template>
