<script setup lang="ts">
import { computed, reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { isAxiosError } from 'axios'
import { Search, X, Music, Eye, Pencil, ChevronDown, Plus } from '@lucide/vue'
import type { Song, SongSheetSummary } from '../types/song'
import { updateSongSheet } from '../apis/songSheetApi'
import SheetViewerModal from './SheetViewerModal.vue'
import type { ViewerSong } from './SheetViewerModal.vue'
import Button from './ui/Button.vue'
import Input from './ui/Input.vue'
import Textarea from './ui/Textarea.vue'
import Label from './ui/Label.vue'

interface ApiErrorResponse { message?: string }
const apiError = (e: unknown, fallback: string) =>
  isAxiosError<ApiErrorResponse>(e) ? (e.response?.data?.message ?? fallback) : fallback

const props = defineProps<{
  songs: Song[]
}>()

const emit = defineEmits<{
  select: [songId: number, songSheetId: number | null]
  close: []
}>()

const query = ref('')
const selectedTag = ref<string | null>(null)
const searchInput = ref<HTMLInputElement | null>(null)
// 곡을 펼치면 그 곡의 악보 버전 목록이 바로 아래에 나타난다(단계 전환 없음)
const expandedSongId = ref<number | null>(null)

const allTags = computed(() => {
  const set = new Set<string>()
  props.songs.forEach(s => s.tags?.forEach(t => set.add(t)))
  return [...set].sort()
})

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return props.songs.filter(s => {
    const matchQuery = !q || s.title.toLowerCase().includes(q) || (s.artist ?? '').toLowerCase().includes(q)
    const matchTag = !selectedTag.value || (s.tags ?? []).includes(selectedTag.value)
    return matchQuery && matchTag
  })
})

const sheetsOf = (song: Song): SongSheetSummary[] => song.sheets ?? song.songSheets ?? []

const sheetLabel = (key: string | null, version: string | null) => {
  if (key && version) return `${key} · ${version}`
  return key ?? version ?? '버전명 없음'
}

// 곡 행 클릭: 악보가 없으면 바로 추가, 있으면 버전 목록을 펼친다
const onSongClick = (song: Song) => {
  cancelEditSheet()
  if (sheetsOf(song).length === 0) {
    emit('select', song.songId, null)
    return
  }
  expandedSongId.value = expandedSongId.value === song.songId ? null : song.songId
}

const addWithSheet = (songId: number, songSheetId: number | null) => {
  emit('select', songId, songSheetId)
}

// 악보 미리보기 (SheetViewerModal 재사용)
// 클릭한 버전 하나만이 아니라 그 곡의 모든 악보 버전을 넘겨보며 바로 고를 수 있도록,
// 곡의 전체 버전을 songs 배열로 넘기고 클릭한 버전의 인덱스로 초기 위치를 잡는다.
const viewerSongs = ref<ViewerSong[]>([])
const viewerSongId = ref<number | null>(null)
const viewerInitialIndex = ref(0)

const openViewer = (song: Song, sheet: SongSheetSummary) => {
  const sheets = sheetsOf(song)
  viewerSongId.value = song.songId
  viewerInitialIndex.value = Math.max(sheets.findIndex(s => s.songSheetId === sheet.songSheetId), 0)
  viewerSongs.value = sheets.map(s => ({
    title: song.title,
    artist: song.artist,
    sheetKey: s.sheetKey,
    versionName: s.versionName,
    files: (s.files ?? []).map(f => ({
      songFileId: f.songFileId,
      originalFileName: f.originalFileName ?? null,
      contentType: f.contentType ?? null,
    })),
  }))
}

const closeViewer = () => {
  viewerSongs.value = []
  viewerSongId.value = null
}

// 뷰어에서 넘겨보다가 "이 버전 추가"를 누르면 그 자리에서 바로 추가한다.
const onViewerSelect = (index: number) => {
  const song = props.songs.find(s => s.songId === viewerSongId.value)
  const sheet = song ? sheetsOf(song)[index] : undefined
  if (!song || !sheet) return
  closeViewer()
  addWithSheet(song.songId, sheet.songSheetId)
}

// 악보 인라인 수정
const editingSheetId = ref<number | null>(null)
const editForm = reactive({ sheetKey: '', versionName: '', memo: '' })
const isSavingSheet = ref(false)
const editSheetError = ref('')

const startEditSheet = (sheet: SongSheetSummary) => {
  editingSheetId.value = sheet.songSheetId
  editForm.sheetKey = sheet.sheetKey ?? ''
  editForm.versionName = sheet.versionName ?? ''
  editForm.memo = sheet.memo ?? ''
  editSheetError.value = ''
}

const cancelEditSheet = () => {
  editingSheetId.value = null
}

const saveEditSheet = async (sheet: SongSheetSummary) => {
  isSavingSheet.value = true
  editSheetError.value = ''
  try {
    const updated = await updateSongSheet(sheet.songSheetId, {
      sheetKey: editForm.sheetKey.trim() || null,
      versionName: editForm.versionName.trim() || null,
      memo: editForm.memo.trim() || null,
    })
    sheet.sheetKey = updated.sheetKey
    sheet.versionName = updated.versionName
    sheet.memo = updated.memo
    editingSheetId.value = null
  } catch (e) {
    editSheetError.value = apiError(e, '악보 수정에 실패했습니다.')
  } finally {
    isSavingSheet.value = false
  }
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (viewerSongs.value.length > 0) return // 뷰어가 열려있으면 뷰어만 닫히도록 피커는 유지
    emit('close')
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  nextTick(() => searchInput.value?.focus())
})
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <!-- 백드롭 -->
    <div class="absolute inset-0 bg-black/60" @click="emit('close')" />

    <!-- 모달 -->
    <div class="relative w-full max-w-lg bg-card border border-border rounded-xl shadow-xl flex flex-col max-h-[80dvh]">
      <!-- 헤더 -->
      <div class="flex items-center justify-between px-5 pt-4 pb-3 border-b border-border shrink-0">
        <h2 class="text-sm font-semibold text-foreground">곡 선택</h2>
        <button type="button" class="text-muted-foreground hover:text-foreground" @click="emit('close')">
          <X class="w-4 h-4" />
        </button>
      </div>

      <!-- 검색 -->
      <div class="px-4 py-3 border-b border-border shrink-0 flex flex-col gap-2">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <input
            ref="searchInput"
            v-model="query"
            type="text"
            placeholder="제목 또는 아티스트 검색"
            class="w-full pl-9 pr-3 py-2 text-sm bg-muted rounded-md text-foreground placeholder:text-muted-foreground outline-none focus:ring-2 focus:ring-primary/50"
          />
        </div>
        <div v-if="allTags.length > 0" class="flex flex-wrap gap-1.5">
          <button
            v-for="tag in allTags"
            :key="tag"
            type="button"
            class="inline-flex items-center h-6 px-2 rounded-full text-xs font-medium border transition-colors"
            :class="selectedTag === tag
              ? 'bg-primary text-primary-foreground border-primary'
              : 'bg-muted text-muted-foreground border-border hover:border-primary hover:text-primary'"
            @click="selectedTag = selectedTag === tag ? null : tag"
          >
            {{ tag }}
          </button>
        </div>
      </div>

      <!-- 곡 목록 (곡 클릭 시 그 자리에서 버전 펼침) -->
      <div class="overflow-y-auto flex-1">
        <p v-if="filtered.length === 0" class="text-sm text-muted-foreground text-center py-10">
          검색 결과가 없습니다.
        </p>

        <div v-for="song in filtered" :key="song.songId" class="border-b border-border last:border-0">
          <!-- 곡 행 -->
          <button
            type="button"
            class="w-full flex items-center gap-3 px-5 py-3 hover:bg-muted/60 transition-colors text-left"
            :class="expandedSongId === song.songId ? 'bg-muted/40' : ''"
            @click="onSongClick(song)"
          >
            <div class="w-8 h-8 rounded-lg bg-muted flex items-center justify-center shrink-0">
              <Music class="w-4 h-4 text-muted-foreground" />
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-foreground truncate">{{ song.title }}</p>
              <p v-if="song.artist" class="text-xs text-muted-foreground truncate">{{ song.artist }}</p>
            </div>
            <span v-if="sheetsOf(song).length > 0" class="text-xs text-muted-foreground shrink-0">
              {{ sheetsOf(song).length }}개 버전
            </span>
            <!-- 악보 없는 곡은 바로 추가되므로 + 아이콘, 있으면 펼침 화살표 -->
            <Plus v-if="sheetsOf(song).length === 0" class="w-4 h-4 text-muted-foreground shrink-0" />
            <ChevronDown
              v-else
              class="w-4 h-4 text-muted-foreground shrink-0 transition-transform"
              :class="expandedSongId === song.songId ? 'rotate-180' : ''"
            />
          </button>

          <!-- 펼쳐진 버전 목록 -->
          <div v-if="expandedSongId === song.songId" class="px-5 pb-3 pt-1 bg-muted/20 flex flex-col gap-1.5">
            <!-- 버전 없이 추가 -->
            <button
              type="button"
              class="w-full flex items-center gap-2 px-3 py-2 rounded-md border border-border hover:border-primary hover:bg-primary/5 transition-colors text-left"
              @click="addWithSheet(song.songId, null)"
            >
              <Plus class="w-3.5 h-3.5 text-primary shrink-0" />
              <span class="text-sm text-foreground">버전 없이 추가</span>
            </button>

            <template v-for="sheet in sheetsOf(song)" :key="sheet.songSheetId">
              <!-- 인라인 수정 폼 -->
              <div v-if="editingSheetId === sheet.songSheetId" class="p-3 rounded-md border border-primary bg-primary/5 flex flex-col gap-2">
                <p v-if="editSheetError" class="text-xs text-destructive bg-destructive-soft rounded-md px-2 py-1.5">{{ editSheetError }}</p>
                <div class="grid grid-cols-2 gap-2">
                  <div class="flex flex-col gap-1">
                    <Label class="text-xs">키</Label>
                    <Input v-model="editForm.sheetKey" type="text" placeholder="예: G" />
                  </div>
                  <div class="flex flex-col gap-1">
                    <Label class="text-xs">버전명</Label>
                    <Input v-model="editForm.versionName" type="text" placeholder="예: 인도자용" />
                  </div>
                </div>
                <div class="flex flex-col gap-1">
                  <Label class="text-xs">메모</Label>
                  <Textarea v-model="editForm.memo" rows="2" />
                </div>
                <div class="flex gap-2">
                  <Button size="sm" :disabled="isSavingSheet" @click="saveEditSheet(sheet)">
                    {{ isSavingSheet ? '저장 중...' : '저장' }}
                  </Button>
                  <Button size="sm" variant="outline" :disabled="isSavingSheet" @click="cancelEditSheet">취소</Button>
                </div>
              </div>

              <!-- 버전 행: 클릭하면 그 버전으로 바로 추가 -->
              <div v-else class="flex items-stretch gap-1.5">
                <button
                  type="button"
                  class="flex-1 flex items-center gap-2 px-3 py-2 rounded-md border border-border hover:border-primary hover:bg-primary/5 transition-colors text-left"
                  @click="addWithSheet(song.songId, sheet.songSheetId)"
                >
                  <Plus class="w-3.5 h-3.5 text-primary shrink-0" />
                  <span class="text-sm font-medium text-foreground">{{ sheetLabel(sheet.sheetKey, sheet.versionName) }}</span>
                  <span v-if="(sheet.files ?? []).length > 0" class="ml-auto text-xs text-muted-foreground">
                    {{ (sheet.files ?? []).length }}장
                  </span>
                </button>
                <button
                  type="button"
                  class="shrink-0 inline-flex items-center px-2.5 rounded-md border border-border text-muted-foreground hover:bg-muted/60 hover:text-foreground transition-colors"
                  title="악보 수정"
                  @click.stop="startEditSheet(sheet)"
                >
                  <Pencil class="w-4 h-4" />
                </button>
                <button
                  v-if="(sheet.files ?? []).length > 0"
                  type="button"
                  class="shrink-0 inline-flex items-center px-2.5 rounded-md border border-border text-muted-foreground hover:bg-muted/60 hover:text-foreground transition-colors"
                  title="악보 보기"
                  @click.stop="openViewer(song, sheet)"
                >
                  <Eye class="w-4 h-4" />
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 악보 미리보기 (피커 위에 오버레이). 같은 곡의 버전들을 넘겨보다가 바로 추가할 수 있다 -->
  <SheetViewerModal
    v-if="viewerSongs.length > 0"
    :songs="viewerSongs"
    :setlist-title="null"
    :initial-index="viewerInitialIndex"
    selectable
    @select="onViewerSelect"
    @close="closeViewer"
  />
</template>
