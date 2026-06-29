<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { ChevronLeft, Pencil, Trash2, Plus, X, BookOpen, Share2, Link, Link2Off } from '@lucide/vue'
import { deleteSetlist, updateSetlist, generateShareToken, revokeShareToken } from '../apis/setlistApi'
import { addSetlistItem, deleteSetlistItem } from '../apis/setlistItemApi'
import { getSong } from '../apis/songApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import SheetViewerModal from '../components/SheetViewerModal.vue'
import type { ViewerSong } from '../components/SheetViewerModal.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import Badge from '../components/ui/Badge.vue'
import Card from '../components/ui/Card.vue'
import Select from '../components/ui/Select.vue'
import { useSetlistStore } from '../stores/setlistStore'
import { useSongStore } from '../stores/songStore'
import type { SongSheetSummary } from '../types/song'

interface ApiErrorResponse { message?: string }

const props = defineProps<{ setlistId: number }>()
const router = useRouter()
const store = useSetlistStore()
const songStore = useSongStore()

const setlist = computed(() => store.selectedSetlist)
const items = computed(() => [...(setlist.value?.items ?? [])].sort((a, b) => a.orderNo - b.orderNo))

const apiError = (e: unknown, fallback: string) =>
  isAxiosError<ApiErrorResponse>(e) ? (e.response?.data?.message ?? fallback) : fallback

// ── 셋리스트 수정
const isEditing = ref(false)
const editForm = reactive({ serviceDate: '', serviceType: '', title: '', memo: '' })
const editError = ref('')
const isSavingEdit = ref(false)

const startEdit = () => {
  editForm.serviceDate = setlist.value?.serviceDate ?? ''
  editForm.serviceType = setlist.value?.serviceType ?? ''
  editForm.title = setlist.value?.title ?? ''
  editForm.memo = setlist.value?.memo ?? ''
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
      serviceType: editForm.serviceType.trim() || null,
      title: editForm.title.trim() || null,
      memo: editForm.memo.trim() || null,
    })
    await store.fetchSetlist(props.setlistId)
    isEditing.value = false
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

// ── 곡 추가
const showAddItem = ref(false)
const addForm = reactive({ songId: null as number | null, songSheetId: null as number | null, memo: '' })
const addError = ref('')
const isAddingItem = ref(false)
const availableSheets = ref<SongSheetSummary[]>([])
const isLoadingSheets = ref(false)

watch(() => addForm.songId, async (songId) => {
  addForm.songSheetId = null
  availableSheets.value = []
  if (!songId) return
  isLoadingSheets.value = true
  try {
    const song = await getSong(songId)
    availableSheets.value = song.sheets ?? song.songSheets ?? []
  } finally {
    isLoadingSheets.value = false
  }
})

const resetAddForm = () => {
  addForm.songId = null
  addForm.songSheetId = null
  addForm.memo = ''
  addError.value = ''
  availableSheets.value = []
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
    })
    resetAddForm()
    showAddItem.value = false
    await store.fetchSetlist(props.setlistId)
  } catch (e) {
    addError.value = apiError(e, '곡 추가에 실패했습니다.')
  } finally {
    isAddingItem.value = false
  }
}

// ── 곡 삭제
const handleDeleteItem = async (itemId: number, songTitle: string) => {
  if (!confirm(`"${songTitle}"을 콘티에서 제거할까요?`)) return
  try {
    await deleteSetlistItem(itemId)
    await store.fetchSetlist(props.setlistId)
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
      return {
        title: item.songTitle,
        artist: item.songArtist,
        sheetKey: item.sheetKey,
        versionName: item.versionName,
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

const load = () => {
  if (Number.isFinite(props.setlistId)) {
    void store.fetchSetlist(props.setlistId)
    void songStore.fetchSongs()
  }
}

onMounted(load)
watch(() => props.setlistId, load)
</script>

<template>
  <DefaultLayout>
    <SheetViewerModal
      v-if="showViewer"
      :songs="viewerSongs"
      :setlist-title="setlist?.title ?? setlist?.serviceDate ?? null"
      @close="showViewer = false"
    />

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
              <!-- 날짜 / 예배 종류 / 제목 -->
              <div class="flex items-start justify-between gap-3 mb-3">
                <div class="min-w-0">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <span class="text-xs text-muted-foreground font-medium">{{ formatDate(setlist.serviceDate) }}</span>
                    <Badge v-if="setlist.serviceType" variant="blue" class="text-xs">{{ setlist.serviceType }}</Badge>
                  </div>
                  <h1 class="text-lg font-bold text-foreground leading-snug">{{ setlist.title ?? '제목 없음' }}</h1>
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
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div class="flex flex-col gap-1.5">
                    <Label for="edit-date">날짜 <span class="text-destructive">*</span></Label>
                    <Input id="edit-date" v-model="editForm.serviceDate" type="date" />
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <Label for="edit-type">예배 종류</Label>
                    <Input id="edit-type" v-model="editForm.serviceType" type="text" />
                  </div>
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-title">제목</Label>
                  <Input id="edit-title" v-model="editForm.title" type="text" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="edit-memo">메모</Label>
                  <Textarea id="edit-memo" v-model="editForm.memo" rows="2" />
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
              <div class="flex flex-col gap-1.5">
                <Label for="add-song">곡 선택 <span class="text-destructive">*</span></Label>
                <Select id="add-song" v-model="addForm.songId">
                  <option :value="null" disabled>곡을 선택하세요</option>
                  <option v-for="song in songStore.songs" :key="song.songId" :value="song.songId">
                    {{ song.title }}{{ song.artist ? ` — ${song.artist}` : '' }}
                  </option>
                </Select>
              </div>
              <div v-if="addForm.songId" class="flex flex-col gap-1.5">
                <Label for="add-sheet">악보 버전</Label>
                <Select id="add-sheet" v-model="addForm.songSheetId" :disabled="isLoadingSheets">
                  <option :value="null">선택 안 함</option>
                  <option v-for="sheet in availableSheets" :key="sheet.songSheetId" :value="sheet.songSheetId">
                    {{ sheetLabel(sheet.sheetKey, sheet.versionName) ?? '버전명 없음' }}
                  </option>
                </Select>
              </div>
              <div class="flex gap-3">
                <div class="flex-1 flex flex-col gap-1.5">
                  <Label for="add-memo">메모</Label>
                  <Input id="add-memo" v-model="addForm.memo" type="text" placeholder="선택사항" />
                </div>
                <div class="flex items-end">
                  <Button :disabled="isAddingItem" @click="handleAddItem">
                    {{ isAddingItem ? '추가 중...' : '추가' }}
                  </Button>
                </div>
              </div>
            </div>
          </Card>

          <!-- 곡 리스트 (스크롤) -->
          <div class="flex-1 min-h-0 overflow-y-auto">
            <p v-if="items.length === 0" class="text-sm text-muted-foreground py-6 text-center">
              곡이 없습니다. 곡을 추가해보세요.
            </p>
            <div v-else class="flex flex-col gap-2 pb-2">
              <Card
                v-for="item in items"
                :key="item.setlistItemId"
                class="px-4 py-3 flex items-center gap-3"
              >
                <div class="w-6 h-6 rounded-full bg-muted flex items-center justify-center shrink-0">
                  <span class="text-xs font-bold text-muted-foreground">{{ item.orderNo }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-sm font-semibold text-foreground">{{ item.songTitle }}</span>
                    <span v-if="item.songArtist" class="text-xs text-muted-foreground">{{ item.songArtist }}</span>
                    <Badge v-if="sheetLabel(item.sheetKey, item.versionName)" variant="violet">
                      {{ sheetLabel(item.sheetKey, item.versionName) }}
                    </Badge>
                  </div>
                  <p v-if="item.memo" class="text-xs text-muted-foreground mt-0.5">{{ item.memo }}</p>
                </div>
                <button
                  type="button"
                  class="p-1.5 text-muted-foreground hover:text-destructive transition-colors shrink-0"
                  @click="handleDeleteItem(item.setlistItemId, item.songTitle)"
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
