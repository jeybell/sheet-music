<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { addSetlistItem, deleteSetlistItem, updateSetlistItem } from '../apis/setlistItemApi'
import { useSetlistFavorites } from '../composables/useSetlistFavorites'
import { ChevronLeft, Pencil, Trash2, Plus, X, BookOpen, Share2, Link, Link2Off, Music, GripVertical, QrCode, Copy, Download, Presentation, Star, KeyRound, Check, PlayCircle} from '@lucide/vue'
import QRCode from 'qrcode'
import { deleteSetlist, updateSetlist, generateShareToken, revokeShareToken, reorderSetlistItems, duplicateSetlist } from '../apis/setlistApi'
import { getSong } from '../apis/songApi'
import { useToast } from '../composables/useToast'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import SheetViewerModal from '../components/SheetViewerModal.vue'
import SongPickerModal from '../components/SongPickerModal.vue'
import type { ViewerSong } from '../components/SheetViewerModal.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import DatePicker from '../components/ui/DatePicker.vue'
import Badge from '../components/ui/Badge.vue'
import Card from '../components/ui/Card.vue'
import { useSetlistStore } from '../stores/setlistStore'
import { useSongStore } from '../stores/songStore'

interface ApiErrorResponse { message?: string }

const props = defineProps<{ setlistId: number }>()
const router = useRouter()
const store = useSetlistStore()
const songStore = useSongStore()

const toast = useToast()
const { isFavorite, toggleFavorite, addRecent } = useSetlistFavorites()
const setlist = computed(() => store.selectedSetlist)
const items = ref<NonNullable<typeof store.selectedSetlist>['items']>([])

watch(
  () => store.selectedSetlist?.items,
  (raw) => {
    items.value = [...(raw ?? [])].sort((a, b) => a.orderNo - b.orderNo)
  },
  { immediate: true, deep: true }
)

// ── 드래그앤드롭 순서 변경 (Pointer Events 기반: 마우스/터치/펜 공통 지원)
const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

const onHandlePointerDown = (e: PointerEvent, index: number) => {
  dragIndex.value = index
  dragOverIndex.value = index
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}

const onHandlePointerMove = (e: PointerEvent) => {
  if (dragIndex.value === null) return
  e.preventDefault()
  const target = document.elementFromPoint(e.clientX, e.clientY)
  const card = target?.closest<HTMLElement>('[data-drag-index]')
  if (card) {
    dragOverIndex.value = Number(card.dataset.dragIndex)
  }
}

const finishDrag = async () => {
  const from = dragIndex.value
  const to = dragOverIndex.value
  dragIndex.value = null
  dragOverIndex.value = null
  if (from === null || to === null || from === to) return

  const reordered = [...items.value]
  const [moved] = reordered.splice(from, 1)
  reordered.splice(to, 0, moved)
  items.value = reordered

  try {
    await reorderSetlistItems(props.setlistId, reordered.map(i => i.setlistItemId))
    await store.fetchSetlist(props.setlistId)
  } catch {
    await store.fetchSetlist(props.setlistId)
  }
}

const onHandlePointerUp = (e: PointerEvent) => {
  ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
  void finishDrag()
}

const onHandlePointerCancel = () => {
  dragIndex.value = null
  dragOverIndex.value = null
}

const apiError = (e: unknown, fallback: string) =>
  isAxiosError<ApiErrorResponse>(e) ? (e.response?.data?.message ?? fallback) : fallback

// ── 셋리스트 수정
const isEditing = ref(false)
const editForm = reactive({ serviceDate: '', title: '', memo: '', youtubeUrl: '' })
const editError = ref('')
const isSavingEdit = ref(false)

const startEdit = () => {
  editForm.serviceDate = setlist.value?.serviceDate ?? ''
  editForm.title = setlist.value?.title ?? ''
  editForm.memo = setlist.value?.memo ?? ''
  editForm.youtubeUrl = setlist.value?.youtubeUrl ?? ''
  editError.value = ''
  isEditing.value = true
}

const handleUpdate = async () => {
  if (!editForm.serviceDate) {
    editError.value = '날짜는 필수입니다.'
    return
  }
  isSavingEdit.value = true
  editError.value = ''
  try {
    await updateSetlist(props.setlistId, {
      serviceDate: editForm.serviceDate,
      title: editForm.title.trim() || null,
      memo: editForm.memo.trim() || null,
      youtubeUrl: editForm.youtubeUrl.trim() || null,
    })
    await store.fetchSetlist(props.setlistId)
    isEditing.value = false
    toast.success('콘티를 저장했어요')
  } catch (e) {
    editError.value = apiError(e, '수정에 실패했습니다.')
  } finally {
    isSavingEdit.value = false
  }
}

// ── 셋리스트 삭제
const handleDelete = async () => {
  const label = setlist.value?.title ?? setlist.value?.serviceDate ?? '이 콘티'
  if (!confirm(`"${label}"을 삭제할까요?`)) return
  try {
    await deleteSetlist(props.setlistId)
    await router.push('/setlists')
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 셋리스트 복사 (템플릿으로 재사용)
const showDuplicateModal = ref(false)
const duplicateDate = ref('')
const duplicateError = ref('')
const isDuplicating = ref(false)

const openDuplicateModal = () => {
  duplicateDate.value = setlist.value?.serviceDate ?? ''
  duplicateError.value = ''
  showDuplicateModal.value = true
}

const handleDuplicate = async () => {
  if (!duplicateDate.value) {
    duplicateError.value = '날짜는 필수입니다.'
    return
  }
  isDuplicating.value = true
  duplicateError.value = ''
  try {
    const created = await duplicateSetlist(props.setlistId, duplicateDate.value)
    showDuplicateModal.value = false
    toast.success('콘티를 복사했어요')
    await router.push(`/setlists/${created.setlistId}`)
  } catch (e) {
    duplicateError.value = apiError(e, '복사에 실패했습니다.')
  } finally {
    isDuplicating.value = false
  }
}

// ── 곡 추가
const showAddItem = ref(false)
const showSongPicker = ref(false)
const addForm = reactive({ songId: null as number | null, songSheetId: null as number | null, songTitle: '', memo: '', performanceKey: '', youtubeUrl: '' })
const addError = ref('')
const isAddingItem = ref(false)

const openSongPicker = () => {
  void songStore.ensureSongsLoaded()
  showSongPicker.value = true
}

const onSongPicked = (songId: number, songSheetId: number | null) => {
  const song = songStore.songs.find(s => s.songId === songId)
  addForm.songId = songId
  addForm.songSheetId = songSheetId
  addForm.songTitle = song?.title ?? ''
  showSongPicker.value = false
}

const resetAddForm = () => {
  addForm.songId = null
  addForm.songSheetId = null
  addForm.songTitle = ''
  addForm.memo = ''
  addForm.performanceKey = ''
  addForm.youtubeUrl = ''
  addError.value = ''
}

const handleAddItem = async () => {
  if (!addForm.songId) {
    addError.value = '곡을 선택해주세요.'
    return
  }
  isAddingItem.value = true
  addError.value = ''
  try {
    await addSetlistItem(props.setlistId, {
      songId: addForm.songId,
      songSheetId: addForm.songSheetId ?? undefined,
      orderNo: items.value.length + 1,
      memo: addForm.memo.trim() || null,
      performanceKey: addForm.performanceKey.trim() || null,
      youtubeUrl: addForm.youtubeUrl.trim() || null,
    })
    resetAddForm()
    showAddItem.value = false
    await store.fetchSetlist(props.setlistId)
    toast.success('곡을 추가했어요')
  } catch (e) {
    addError.value = apiError(e, '곡 추가에 실패했습니다.')
  } finally {
    isAddingItem.value = false
  }
}

// ── 연주 키(이번 예배 키) 인라인 수정
const editingKeyItemId = ref<number | null>(null)
const keyEditValue = ref('')
const isSavingKey = ref(false)

const startEditKey = (item: (typeof items.value)[number]) => {
  editingKeyItemId.value = item.setlistItemId
  keyEditValue.value = item.performanceKey ?? ''
}

const cancelEditKey = () => {
  editingKeyItemId.value = null
}

const saveKey = async (item: (typeof items.value)[number]) => {
  isSavingKey.value = true
  try {
    await updateSetlistItem(item.setlistItemId, {
      songSheetId: item.songSheetId,
      orderNo: item.orderNo,
      memo: item.memo,
      performanceKey: keyEditValue.value.trim() || null,
      youtubeUrl: item.youtubeUrl,
    })
    editingKeyItemId.value = null
    await store.fetchSetlist(props.setlistId)
  } catch (e) {
    alert(apiError(e, '연주 키 저장에 실패했습니다.'))
  } finally {
    isSavingKey.value = false
  }
}

// ── 곡별 YouTube 링크 인라인 수정
const editingYtItemId = ref<number | null>(null)
const ytEditValue = ref('')
const isSavingYt = ref(false)

const startEditYt = (item: (typeof items.value)[number]) => {
  editingYtItemId.value = item.setlistItemId
  ytEditValue.value = item.youtubeUrl ?? ''
}

const cancelEditYt = () => {
  editingYtItemId.value = null
}

const saveYt = async (item: (typeof items.value)[number]) => {
  isSavingYt.value = true
  try {
    await updateSetlistItem(item.setlistItemId, {
      songSheetId: item.songSheetId,
      orderNo: item.orderNo,
      memo: item.memo,
      performanceKey: item.performanceKey,
      youtubeUrl: ytEditValue.value.trim() || null,
    })
    editingYtItemId.value = null
    await store.fetchSetlist(props.setlistId)
  } catch (e) {
    alert(apiError(e, 'YouTube 링크 저장에 실패했습니다.'))
  } finally {
    isSavingYt.value = false
  }
}

// ── 곡별 메모 인라인 수정
const editingMemoItemId = ref<number | null>(null)
const memoEditValue = ref('')
const isSavingMemo = ref(false)

const startEditMemo = (item: (typeof items.value)[number]) => {
  editingMemoItemId.value = item.setlistItemId
  memoEditValue.value = item.memo ?? ''
}

const cancelEditMemo = () => {
  editingMemoItemId.value = null
}

const saveMemo = async (item: (typeof items.value)[number]) => {
  isSavingMemo.value = true
  try {
    await updateSetlistItem(item.setlistItemId, {
      songSheetId: item.songSheetId,
      orderNo: item.orderNo,
      memo: memoEditValue.value.trim() || null,
      performanceKey: item.performanceKey,
      youtubeUrl: item.youtubeUrl,
    })
    editingMemoItemId.value = null
    await store.fetchSetlist(props.setlistId)
  } catch (e) {
    alert(apiError(e, '메모 저장에 실패했습니다.'))
  } finally {
    isSavingMemo.value = false
  }
}

// ── YouTube 링크 헬퍼 (song 상세와 동일한 방식)
const extractYoutubeId = (url: string): string | null => {
  try {
    const u = new URL(url)
    if (u.hostname === 'youtu.be') return u.pathname.slice(1).split('?')[0] || null
    if (u.hostname.includes('youtube.com')) return u.searchParams.get('v')
  } catch { /* 잘못된 URL 무시 */ }
  return null
}

const openLink = (url: string) => { window.open(url, '_blank', 'noopener') }

// 콘티 전체 YouTube 임베드 토글
const showSetlistEmbed = ref(false)
const setlistYoutubeId = computed(() => {
  const url = setlist.value?.youtubeUrl
  return url ? extractYoutubeId(url) : null
})

// ── 곡 삭제
const handleDeleteItem = async (itemId: number, songTitle: string) => {
  if (!confirm(`"${songTitle}"을 콘티에서 제거할까요?`)) return
  try {
    await deleteSetlistItem(itemId)
    await store.fetchSetlist(props.setlistId)
    toast.success('곡을 제거했어요')
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${m}월 ${d}일`
}

const sheetLabel = (key: string | null, version: string | null) => {
  if (key && version) return `${key} · ${version}`
  return key ?? version ?? null
}

// ── 악보 뷰어
const showViewer = ref(false)
const viewerSongs = ref<ViewerSong[]>([])
const isLoadingViewer = ref(false)

const openViewer = async () => {
  isLoadingViewer.value = true
  try {
    const songIds = [...new Set(items.value.map(i => i.songId))]
    const songMap = new Map<number, Awaited<ReturnType<typeof getSong>>>()
    await Promise.all(songIds.map(async id => {
      const song = await getSong(id)
      songMap.set(id, song)
    }))

    viewerSongs.value = items.value.map(item => {
      const song = songMap.get(item.songId)
      const sheets = song?.sheets ?? song?.songSheets ?? []
      const sheet = item.songSheetId
        ? sheets.find(s => s.songSheetId === item.songSheetId)
        : sheets[0]
      const sheetDeleted = !!item.songSheetId && !sheet
      return {
        title: item.songTitle,
        artist: item.songArtist,
        sheetKey: item.sheetKey,
        versionName: item.versionName,
        sheetDeleted,
        files: (sheet?.files ?? []).map(f => ({
          songFileId: f.songFileId,
          originalFileName: f.originalFileName ?? null,
          contentType: f.contentType ?? null,
        })),
      }
    })
    showViewer.value = true
  } finally {
    isLoadingViewer.value = false
  }
}

// ── 공유 링크
const isGeneratingShare = ref(false)
const shareUrl = computed(() => {
  const token = setlist.value?.shareToken
  if (!token) return null
  return `${window.location.origin}/share/${token}`
})

const handleGenerateShare = async () => {
  isGeneratingShare.value = true
  try {
    await generateShareToken(props.setlistId)
    await store.fetchSetlist(props.setlistId)
  } catch {
    alert('공유 링크 생성에 실패했습니다.')
  } finally {
    isGeneratingShare.value = false
  }
}

const handleRevokeShare = async () => {
  if (!confirm('공유 링크를 비활성화할까요? 기존 링크로는 접근할 수 없게 됩니다.')) return
  try {
    await revokeShareToken(props.setlistId)
    await store.fetchSetlist(props.setlistId)
  } catch {
    alert('공유 링크 비활성화에 실패했습니다.')
  }
}

const copyShareUrl = async () => {
  if (!shareUrl.value) return
  await navigator.clipboard.writeText(shareUrl.value)
  alert('링크가 복사되었습니다.')
}

// ── 공유 QR코드
const showQrModal = ref(false)
const qrCanvas = ref<HTMLCanvasElement | null>(null)

const openQrModal = async () => {
  if (!shareUrl.value) return
  showQrModal.value = true
  await nextTick()
  if (qrCanvas.value) {
    await QRCode.toCanvas(qrCanvas.value, shareUrl.value, { width: 240, margin: 2 })
  }
}

const downloadQr = () => {
  if (!qrCanvas.value) return
  const link = document.createElement('a')
  link.download = `${setlist.value?.title ?? setlist.value?.serviceDate ?? 'setlist'}-qr.png`
  link.href = qrCanvas.value.toDataURL('image/png')
  link.click()
}

const load = () => {
  if (Number.isFinite(props.setlistId)) {
    void store.fetchSetlist(props.setlistId)
    addRecent(props.setlistId)
  }
}

onMounted(load)
watch(() => props.setlistId, load)
</script>

<template>
  <DefaultLayout>
    <SongPickerModal
      v-if="showSongPicker"
      :songs="songStore.songs"
      @select="onSongPicked"
      @close="showSongPicker = false"
    />

    <SheetViewerModal
      v-if="showViewer"
      :songs="viewerSongs"
      :setlist-title="setlist?.title ?? setlist?.serviceDate ?? null"
      @close="showViewer = false"
    />

    <!-- 콘티 복사 모달 -->
    <div v-if="showDuplicateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60" @click="showDuplicateModal = false" />
      <div class="relative w-full max-w-xs bg-card border border-border rounded-xl shadow-xl p-5 flex flex-col gap-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-semibold text-foreground">콘티 복사</h2>
          <button type="button" class="text-muted-foreground hover:text-foreground" @click="showDuplicateModal = false">
            <X class="w-4 h-4" />
          </button>
        </div>
        <p class="text-xs text-muted-foreground -mt-2">곡 순서·악보 버전을 그대로 복사합니다. 새 날짜만 지정해주세요.</p>
        <p v-if="duplicateError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2">{{ duplicateError }}</p>
        <DatePicker v-model="duplicateDate" inline />
        <Button :disabled="isDuplicating" @click="handleDuplicate">
          {{ isDuplicating ? '복사 중...' : '복사하기' }}
        </Button>
      </div>
    </div>

    <!-- 공유 QR코드 모달 -->
    <div
      v-if="showQrModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div class="absolute inset-0 bg-black/60" @click="showQrModal = false" />
      <div class="relative w-full max-w-xs bg-card border border-border rounded-xl shadow-xl p-5 flex flex-col items-center gap-4">
        <div class="flex items-center justify-between w-full">
          <h2 class="text-sm font-semibold text-foreground">공유 QR코드</h2>
          <button type="button" class="text-muted-foreground hover:text-foreground" @click="showQrModal = false">
            <X class="w-4 h-4" />
          </button>
        </div>
        <canvas ref="qrCanvas" class="rounded-md" />
        <Button class="w-full" @click="downloadQr">
          <Download class="w-3.5 h-3.5" />
          이미지 다운로드
        </Button>
      </div>
    </div>

    <!-- 뒤로가기 -->
    <button
      type="button"
      class="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors mb-4"
      @click="$router.push('/setlists')"
    >
      <ChevronLeft class="w-4 h-4" />
      목록으로
    </button>

    <p v-if="store.isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="store.errorMessage" class="text-sm text-destructive">{{ store.errorMessage }}</p>

    <template v-else-if="setlist">
      <!-- 뷰포트 고정 레이아웃 -->
      <div class="h-[calc(100dvh-7.5rem)] flex flex-col gap-4">

        <!-- 상단: 콘티 정보 + 액션 -->
        <Card class="shrink-0">
          <!-- 보기 모드 -->
          <template v-if="!isEditing">
            <div class="px-5 pt-4 pb-3">
              <!-- 날짜 / 제목 -->
              <div class="flex items-start justify-between gap-3 mb-3">
                <div class="min-w-0">
                  <span class="text-xs text-muted-foreground font-medium mb-1 block">{{ formatDate(setlist.serviceDate) }}</span>
                  <div class="flex items-center gap-1.5">
                    <h1 class="text-lg font-bold text-foreground leading-snug">{{ setlist.title ?? '제목 없음' }}</h1>
                    <button
                      type="button"
                      class="text-muted-foreground hover:text-primary transition-colors shrink-0"
                      :aria-label="isFavorite(setlistId) ? '즐겨찾기 해제' : '즐겨찾기 추가'"
                      @click="toggleFavorite(setlistId)"
                    >
                      <Star class="w-4 h-4" :class="{ 'fill-current text-primary': isFavorite(setlistId) }" />
                    </button>
                  </div>
                  <p v-if="setlist.memo" class="text-xs text-muted-foreground mt-1 line-clamp-2">{{ setlist.memo }}</p>
                </div>
                <!-- 액션 버튼 -->
                <div class="flex gap-1.5 shrink-0">
                  <Button
                    variant="secondary"
                    size="sm"
                    :disabled="isLoadingViewer || items.length === 0"
                    @click="openViewer"
                  >
                    <BookOpen class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">{{ isLoadingViewer ? '로딩 중...' : '악보 보기' }}</span>
                  </Button>
                  <Button variant="outline" size="sm" @click="openDuplicateModal">
                    <Copy class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">복사</span>
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    :disabled="items.length === 0"
                    @click="$router.push(`/setlists/${setlistId}/present`)"
                  >
                    <Presentation class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">진행 모드</span>
                  </Button>
                  <Button variant="outline" size="sm" @click="startEdit">
                    <Pencil class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">수정</span>
                  </Button>
                  <Button variant="destructive" size="sm" @click="handleDelete">
                    <Trash2 class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">삭제</span>
                  </Button>
                </div>
              </div>

              <!-- 콘티 전체 YouTube 링크 -->
              <div v-if="setlist.youtubeUrl" class="pt-3 border-t border-border">
                <div class="flex items-center gap-2">
                  <Button variant="outline" size="sm" @click="openLink(setlist.youtubeUrl!)">
                    <PlayCircle class="w-3.5 h-3.5 text-red-500" />
                    YouTube 열기
                  </Button>
                  <Button
                    v-if="setlistYoutubeId"
                    variant="ghost"
                    size="sm"
                    @click="showSetlistEmbed = !showSetlistEmbed"
                  >
                    {{ showSetlistEmbed ? '접기' : '여기서 재생' }}
                  </Button>
                </div>
                <div v-if="showSetlistEmbed && setlistYoutubeId" class="mt-2">
                  <iframe
                    :src="`https://www.youtube.com/embed/${setlistYoutubeId}`"
                    class="w-full aspect-video rounded-md"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen
                  />
                </div>
              </div>

              <!-- 공유 링크 바 -->
              <div class="pt-3 border-t border-border flex items-center gap-2">
                <template v-if="!shareUrl">
                  <Button variant="outline" size="sm" :disabled="isGeneratingShare" @click="handleGenerateShare">
                    <Share2 class="w-3.5 h-3.5" />
                    {{ isGeneratingShare ? '생성 중...' : '공유 링크 생성' }}
                  </Button>
                </template>
                <template v-else>
                  <Share2 class="w-3.5 h-3.5 text-muted-foreground shrink-0" />
                  <code class="flex-1 text-xs bg-muted px-2 py-1 rounded truncate text-foreground min-w-0">{{ shareUrl }}</code>
                  <Button variant="outline" size="sm" @click="copyShareUrl">
                    <Link class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">복사</span>
                  </Button>
                  <Button variant="outline" size="sm" @click="openQrModal">
                    <QrCode class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">QR코드</span>
                  </Button>
                  <Button variant="outline" size="sm" @click="handleRevokeShare">
                    <Link2Off class="w-3.5 h-3.5" />
                    <span class="hidden sm:inline">비활성화</span>
                  </Button>
                </template>
              </div>
            </div>
          </template>

          <!-- 수정 모드 -->
          <template v-else>
            <div class="px-5 pt-4 pb-4">
              <div class="flex items-center justify-between mb-4">
                <h2 class="text-sm font-semibold text-foreground">콘티 수정</h2>
                <button type="button" class="text-muted-foreground hover:text-foreground" @click="isEditing = false">
                  <X class="w-4 h-4" />
                </button>
              </div>
              <p v-if="editError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4">{{ editError }}</p>
              <div class="flex flex-col gap-3">
                <div class="flex flex-col gap-1.5">
                  <Label>날짜 <span class="text-destructive">*</span></Label>
                  <DatePicker v-model="editForm.serviceDate" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-title">제목</Label>
                  <Input id="edit-title" v-model="editForm.title" type="text" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-memo">메모</Label>
                  <Textarea id="edit-memo" v-model="editForm.memo" rows="2" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-youtube">YouTube 링크 <span class="text-muted-foreground font-normal">(콘티 전체)</span></Label>
                  <Input id="edit-youtube" v-model="editForm.youtubeUrl" type="url" placeholder="https://youtu.be/..." />
                </div>
                <div class="flex gap-2">
                  <Button :disabled="isSavingEdit" @click="handleUpdate">{{ isSavingEdit ? '저장 중...' : '저장' }}</Button>
                  <Button variant="outline" :disabled="isSavingEdit" @click="isEditing = false">취소</Button>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <!-- 하단: 곡 목록 (내부 스크롤) -->
        <div class="flex-1 min-h-0 flex flex-col">
          <!-- 섹션 헤더 -->
          <div class="flex items-center justify-between mb-3 shrink-0">
            <h2 class="text-sm font-semibold text-foreground">
              곡 목록
              <span class="ml-1.5 font-normal text-muted-foreground">{{ items.length }}곡</span>
            </h2>
            <Button variant="outline" size="sm" @click="showAddItem = !showAddItem">
              <template v-if="showAddItem"><X class="w-3.5 h-3.5" />취소</template>
              <template v-else><Plus class="w-3.5 h-3.5" />곡 추가</template>
            </Button>
          </div>

          <!-- 곡 추가 폼 -->
          <Card v-if="showAddItem" class="p-4 mb-3 bg-muted/40 shrink-0">
            <p v-if="addError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-3">{{ addError }}</p>
            <div class="flex flex-col gap-3">
              <!-- 곡 선택 버튼 -->
              <div class="flex flex-col gap-1.5">
                <Label>곡 선택 <span class="text-destructive">*</span></Label>
                <button
                  type="button"
                  class="flex items-center gap-2 w-full px-3 py-2 text-sm rounded-md border border-border bg-background hover:bg-muted transition-colors text-left"
                  @click="openSongPicker"
                >
                  <Music class="w-4 h-4 text-muted-foreground shrink-0" />
                  <span v-if="addForm.songId" class="text-foreground font-medium">{{ addForm.songTitle }}</span>
                  <span v-else class="text-muted-foreground">곡을 검색해서 선택하세요</span>
                </button>
              </div>
              <!-- 연주 키(이번 예배 키) -->
              <div class="flex flex-col gap-1.5">
                <Label for="add-performance-key">연주 키 <span class="text-muted-foreground font-normal">(악보 원래 키와 다를 경우)</span></Label>
                <Input id="add-performance-key" v-model="addForm.performanceKey" type="text" placeholder="예: G" class="max-w-[8rem]" />
              </div>
              <!-- YouTube 링크 (곡별) -->
              <div class="flex flex-col gap-1.5">
                <Label for="add-youtube">YouTube 링크 <span class="text-muted-foreground font-normal">(이 곡 참고 영상)</span></Label>
                <Input id="add-youtube" v-model="addForm.youtubeUrl" type="url" placeholder="https://youtu.be/..." />
              </div>
              <!-- 메모 (여러 줄) -->
              <div class="flex flex-col gap-1.5">
                <Label for="add-memo">메모</Label>
                <Textarea id="add-memo" v-model="addForm.memo" rows="3" placeholder="악보 지시사항, 전조 메모 등 자유롭게 입력" />
              </div>
              <div class="flex gap-2">
                <Button :disabled="isAddingItem || !addForm.songId" @click="handleAddItem">
                  {{ isAddingItem ? '추가 중...' : '추가' }}
                </Button>
                <Button variant="outline" @click="() => { showAddItem = false; resetAddForm() }">취소</Button>
              </div>
            </div>
          </Card>

          <!-- 곡 리스트 (2컬럼 그리드, 내부 스크롤) -->
          <div class="flex-1 min-h-0 overflow-y-auto">
            <p v-if="items.length === 0" class="text-sm text-muted-foreground py-6 text-center">
              곡이 없습니다. 곡을 추가해보세요.
            </p>
            <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-2 pb-2">
              <Card
                v-for="(item, idx) in items"
                :key="item.setlistItemId"
                :data-drag-index="idx"
                class="px-4 py-3 flex items-start gap-3 transition-opacity select-none"
                :class="{
                  'opacity-40': dragIndex === idx,
                  'ring-2 ring-primary ring-offset-1 ring-offset-background': dragOverIndex === idx && dragIndex !== idx,
                }"
              >
                <button
                  type="button"
                  class="p-1 -m-1 text-muted-foreground hover:text-foreground shrink-0 mt-0.5 cursor-grab active:cursor-grabbing touch-none"
                  @pointerdown="onHandlePointerDown($event, idx)"
                  @pointermove="onHandlePointerMove"
                  @pointerup="onHandlePointerUp"
                  @pointercancel="onHandlePointerCancel"
                >
                  <GripVertical class="w-4 h-4" />
                </button>
                <div class="w-6 h-6 rounded-full bg-muted flex items-center justify-center shrink-0 mt-0.5">
                  <span class="text-xs font-bold text-muted-foreground">{{ idx + 1 }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap mb-0.5">
                    <button
                      type="button"
                      class="text-sm font-semibold text-foreground hover:text-primary hover:underline text-left"
                      title="곡 상세 보기"
                      @click.stop="router.push(`/songs/${item.songId}`)"
                    >{{ item.songTitle }}</button>
                    <span v-if="item.songArtist" class="text-xs text-muted-foreground">{{ item.songArtist }}</span>
                  </div>
                  <div class="flex items-center gap-1 flex-wrap mb-1">
                    <Badge v-if="sheetLabel(item.sheetKey, item.versionName)" variant="violet">
                      {{ sheetLabel(item.sheetKey, item.versionName) }}
                    </Badge>
                    <Badge v-else-if="item.songSheetId" variant="destructive" class="text-xs">
                      악보 버전 삭제됨
                    </Badge>

                    <!-- 연주 키(이번 예배 키) 배지 + 인라인 수정 -->
                    <template v-if="editingKeyItemId === item.setlistItemId">
                      <input
                        v-model="keyEditValue"
                        type="text"
                        placeholder="예: G"
                        class="w-16 h-5 px-1.5 text-xs rounded border border-primary bg-background text-foreground"
                        @click.stop
                        @keydown.enter.stop="saveKey(item)"
                        @keydown.esc.stop="cancelEditKey"
                      />
                      <button type="button" class="text-primary" :disabled="isSavingKey" @click.stop="saveKey(item)">
                        <Check class="w-3.5 h-3.5" />
                      </button>
                      <button type="button" class="text-muted-foreground" @click.stop="cancelEditKey">
                        <X class="w-3.5 h-3.5" />
                      </button>
                    </template>
                    <button
                      v-else
                      type="button"
                      class="inline-flex items-center gap-1"
                      @click.stop="startEditKey(item)"
                    >
                      <Badge v-if="item.performanceKey" variant="default">
                        <KeyRound class="w-3 h-3 mr-0.5" />연주 키 {{ item.performanceKey }}
                      </Badge>
                      <span v-else class="inline-flex items-center gap-0.5 text-xs text-muted-foreground hover:text-primary transition-colors">
                        <KeyRound class="w-3 h-3" />연주 키 설정
                      </span>
                    </button>

                    <!-- 곡별 YouTube 링크: 열기 + 인라인 수정 -->
                    <template v-if="editingYtItemId === item.setlistItemId">
                      <input
                        v-model="ytEditValue"
                        type="url"
                        placeholder="https://youtu.be/..."
                        class="w-44 h-5 px-1.5 text-xs rounded border border-primary bg-background text-foreground"
                        @click.stop
                        @keydown.enter.stop="saveYt(item)"
                        @keydown.esc.stop="cancelEditYt"
                      />
                      <button type="button" class="text-primary" :disabled="isSavingYt" @click.stop="saveYt(item)">
                        <Check class="w-3.5 h-3.5" />
                      </button>
                      <button type="button" class="text-muted-foreground" @click.stop="cancelEditYt">
                        <X class="w-3.5 h-3.5" />
                      </button>
                    </template>
                    <template v-else>
                      <button
                        v-if="item.youtubeUrl"
                        type="button"
                        class="inline-flex items-center gap-0.5 text-xs text-red-500 hover:underline"
                        @click.stop="openLink(item.youtubeUrl)"
                      >
                        <PlayCircle class="w-3.5 h-3.5" />YouTube
                      </button>
                      <button
                        v-else
                        type="button"
                        class="inline-flex items-center gap-0.5 text-xs text-muted-foreground hover:text-red-500 transition-colors"
                        @click.stop="startEditYt(item)"
                      >
                        <PlayCircle class="w-3 h-3" />링크 추가
                      </button>
                      <button
                        v-if="item.youtubeUrl"
                        type="button"
                        class="text-muted-foreground hover:text-foreground"
                        aria-label="YouTube 링크 수정"
                        @click.stop="startEditYt(item)"
                      >
                        <Pencil class="w-3 h-3" />
                      </button>
                    </template>
                  </div>
                  <!-- 메모: 표시 + 인라인 수정 -->
                  <template v-if="editingMemoItemId === item.setlistItemId">
                    <textarea
                      v-model="memoEditValue"
                      rows="2"
                      placeholder="메모 입력"
                      class="w-full text-xs px-2 py-1 rounded border border-primary bg-background text-foreground resize-y"
                      @click.stop
                      @keydown.esc.stop="cancelEditMemo"
                    />
                    <div class="flex gap-2 mt-1">
                      <button type="button" class="inline-flex items-center gap-0.5 text-xs text-primary" :disabled="isSavingMemo" @click.stop="saveMemo(item)">
                        <Check class="w-3.5 h-3.5" />저장
                      </button>
                      <button type="button" class="inline-flex items-center gap-0.5 text-xs text-muted-foreground" @click.stop="cancelEditMemo">
                        <X class="w-3.5 h-3.5" />취소
                      </button>
                    </div>
                  </template>
                  <template v-else>
                    <button
                      v-if="item.memo"
                      type="button"
                      class="group/memo text-left w-full"
                      title="메모 수정"
                      @click.stop="startEditMemo(item)"
                    >
                      <span class="text-xs text-muted-foreground whitespace-pre-line leading-relaxed group-hover/memo:text-foreground transition-colors">{{ item.memo }}</span>
                    </button>
                    <button
                      v-else
                      type="button"
                      class="inline-flex items-center gap-0.5 text-xs text-muted-foreground hover:text-primary transition-colors"
                      @click.stop="startEditMemo(item)"
                    >
                      <Pencil class="w-3 h-3" />메모 추가
                    </button>
                  </template>
                </div>
                <button
                  type="button"
                  class="p-1.5 text-muted-foreground hover:text-destructive transition-colors shrink-0"
                  @click.stop="handleDeleteItem(item.setlistItemId, item.songTitle)"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </template>
  </DefaultLayout>
</template>
