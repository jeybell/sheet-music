<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Search, X, Music, Eye } from '@lucide/vue'
import type { Song, SongSheetSummary } from '../types/song'
import SheetViewerModal from './SheetViewerModal.vue'
import type { ViewerSong } from './SheetViewerModal.vue'

const props = defineProps<{
  songs: Song[]
}>()

const emit = defineEmits<{
  select: [songId: number, songSheetId: number | null]
  close: []
}>()

const query = ref('')
const selectedTag = ref<string | null>(null)
const selectedSong = ref<Song | null>(null)
const selectedSheetId = ref<number | null>(null)
const searchInput = ref<HTMLInputElement | null>(null)

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

const sheets = computed<SongSheetSummary[]>(() => {
  if (!selectedSong.value) return []
  return selectedSong.value.sheets ?? selectedSong.value.songSheets ?? []
})

const sheetLabel = (key: string | null, version: string | null) => {
  if (key && version) return `${key} · ${version}`
  return key ?? version ?? '버전명 없음'
}

const pickSong = (song: Song) => {
  selectedSong.value = song
  selectedSheetId.value = null
}

// 악보 미리보기 (SheetViewerModal 재사용)
const viewerSheet = ref<ViewerSong | null>(null)

const openViewer = (sheet: SongSheetSummary) => {
  if (!selectedSong.value) return
  viewerSheet.value = {
    title: selectedSong.value.title,
    artist: selectedSong.value.artist,
    sheetKey: sheet.sheetKey,
    versionName: sheet.versionName,
    files: (sheet.files ?? []).map(f => ({
      songFileId: f.songFileId,
      originalFileName: f.originalFileName ?? null,
      contentType: f.contentType ?? null,
    })),
  }
}

const confirm = () => {
  if (!selectedSong.value) return
  emit('select', selectedSong.value.songId, selectedSheetId.value)
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (viewerSheet.value) return // 뷰어가 열려있으면 뷰어만 닫히도록 피커는 유지
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

      <template v-if="!selectedSong">
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

        <!-- 곡 목록 -->
        <div class="overflow-y-auto flex-1">
          <p v-if="filtered.length === 0" class="text-sm text-muted-foreground text-center py-10">
            검색 결과가 없습니다.
          </p>
          <button
            v-for="song in filtered"
            :key="song.songId"
            type="button"
            class="w-full flex items-center gap-3 px-5 py-3 hover:bg-muted/60 transition-colors text-left border-b border-border last:border-0"
            @click="pickSong(song)"
          >
            <div class="w-8 h-8 rounded-lg bg-muted flex items-center justify-center shrink-0">
              <Music class="w-4 h-4 text-muted-foreground" />
            </div>
            <div class="min-w-0">
              <p class="text-sm font-medium text-foreground truncate">{{ song.title }}</p>
              <p v-if="song.artist" class="text-xs text-muted-foreground truncate">{{ song.artist }}</p>
            </div>
            <span v-if="(song.sheets ?? song.songSheets ?? []).length > 0" class="ml-auto text-xs text-muted-foreground shrink-0">
              {{ (song.sheets ?? song.songSheets ?? []).length }}개 버전
            </span>
          </button>
        </div>
      </template>

      <!-- 악보 버전 선택 -->
      <template v-else>
        <div class="px-5 py-3 border-b border-border shrink-0 flex items-center gap-2">
          <button type="button" class="text-muted-foreground hover:text-foreground" @click="selectedSong = null">
            <X class="w-4 h-4" />
          </button>
          <div>
            <p class="text-sm font-semibold text-foreground">{{ selectedSong.title }}</p>
            <p v-if="selectedSong.artist" class="text-xs text-muted-foreground">{{ selectedSong.artist }}</p>
          </div>
        </div>

        <div class="overflow-y-auto flex-1 px-5 py-4">
          <p class="text-xs text-muted-foreground mb-3">악보 버전 선택 (선택 안 해도 됩니다)</p>

          <!-- 선택 안 함 -->
          <button
            type="button"
            class="w-full flex items-center gap-3 px-4 py-3 rounded-lg mb-2 border transition-colors"
            :class="selectedSheetId === null
              ? 'border-primary bg-primary/10 text-primary'
              : 'border-border hover:bg-muted/60 text-foreground'"
            @click="selectedSheetId = null"
          >
            <span class="text-sm font-medium">선택 안 함</span>
          </button>

          <div
            v-for="sheet in sheets"
            :key="sheet.songSheetId"
            class="flex items-stretch gap-2 mb-2"
          >
            <button
              type="button"
              class="flex-1 flex items-center gap-3 px-4 py-3 rounded-lg border transition-colors"
              :class="selectedSheetId === sheet.songSheetId
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-border hover:bg-muted/60 text-foreground'"
              @click="selectedSheetId = sheet.songSheetId"
            >
              <span class="text-sm font-medium">{{ sheetLabel(sheet.sheetKey, sheet.versionName) }}</span>
              <span v-if="(sheet.files ?? []).length > 0" class="ml-auto text-xs text-muted-foreground">
                {{ (sheet.files ?? []).length }}장
              </span>
            </button>
            <button
              v-if="(sheet.files ?? []).length > 0"
              type="button"
              class="shrink-0 inline-flex items-center gap-1 px-3 rounded-lg border border-border text-xs font-medium text-muted-foreground hover:bg-muted/60 hover:text-foreground transition-colors"
              title="악보 보기"
              @click="openViewer(sheet)"
            >
              <Eye class="w-4 h-4" />
              악보
            </button>
          </div>

          <p v-if="sheets.length === 0" class="text-sm text-muted-foreground text-center py-4">
            등록된 악보 버전이 없습니다.
          </p>
        </div>

        <div class="px-5 py-3 border-t border-border shrink-0">
          <button
            type="button"
            class="w-full bg-primary text-primary-foreground text-sm font-medium py-2.5 rounded-lg hover:bg-primary/90 transition-colors"
            @click="confirm"
          >
            선택 완료
          </button>
        </div>
      </template>
    </div>
  </div>

  <!-- 악보 미리보기 (피커 위에 오버레이) -->
  <SheetViewerModal
    v-if="viewerSheet"
    :songs="[viewerSheet]"
    :setlist-title="null"
    @close="viewerSheet = null"
  />
</template>
