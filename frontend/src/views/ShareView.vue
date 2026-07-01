<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Music, BookOpen, Home } from '@lucide/vue'
import { getSharedSetlist } from '../apis/setlistApi'
import type { SharedSetlist, SharedSetlistItem } from '../types/setlist'
import SheetViewerModal from '../components/SheetViewerModal.vue'
import type { ViewerSong } from '../components/SheetViewerModal.vue'

const route = useRoute()
const token = route.params.token as string

const setlist = ref<SharedSetlist | null>(null)
const isLoading = ref(true)
const errorMessage = ref('')

const sheetLabel = (item: SharedSetlistItem) => {
  if (item.sheetKey && item.versionName) return `${item.sheetKey} · ${item.versionName}`
  return item.sheetKey ?? item.versionName ?? null
}

const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${m}월 ${d}일`
}

// ── 악보 풀스크린 뷰어 (SheetViewerModal 재사용)
const showViewer = ref(false)
const viewerSongs = computed<ViewerSong[]>(() =>
  (setlist.value?.items ?? []).map((item) => ({
    title: item.songTitle,
    artist: item.songArtist,
    sheetKey: item.sheetKey,
    versionName: item.versionName,
    files: item.files.map((f) => ({
      songFileId: f.songFileId,
      originalFileName: f.originalFileName,
      contentType: f.contentType,
    })),
  })),
)
const totalFileCount = computed(() =>
  (setlist.value?.items ?? []).reduce((sum, item) => sum + item.files.length, 0),
)

onMounted(async () => {
  try {
    setlist.value = await getSharedSetlist(token)
  } catch {
    errorMessage.value = '공유 링크가 유효하지 않거나 만료되었습니다.'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div class="min-h-dvh bg-background text-foreground">
    <!-- 헤더 -->
    <header class="border-b border-border px-4 py-3 flex items-center gap-2">
      <Music class="w-5 h-5 text-primary" />
      <span class="font-semibold text-sm text-foreground">악보 공유</span>
      <RouterLink
        to="/"
        class="ml-auto inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
      >
        <Home class="w-3.5 h-3.5" />
        앱 홈으로
      </RouterLink>
    </header>

    <main class="max-w-3xl mx-auto px-4 py-6">
      <!-- 로딩 -->
      <p v-if="isLoading" class="text-sm text-muted-foreground text-center py-16">불러오는 중...</p>

      <!-- 에러 -->
      <div v-else-if="errorMessage" class="py-16 text-center">
        <p class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>

      <template v-else-if="setlist">
        <!-- 콘티 정보 -->
        <div class="mb-6 flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs text-muted-foreground mb-1">{{ formatDate(setlist.serviceDate) }}</p>
            <h1 class="text-xl font-bold text-foreground">{{ setlist.title ?? '콘티' }}</h1>
            <p v-if="setlist.memo" class="text-sm text-muted-foreground mt-1">{{ setlist.memo }}</p>
          </div>
          <button
            v-if="totalFileCount > 0"
            type="button"
            class="shrink-0 inline-flex items-center gap-1.5 h-9 px-4 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:opacity-90 transition-opacity"
            @click="showViewer = true"
          >
            <BookOpen class="w-4 h-4" />
            악보 보기
          </button>
        </div>

        <!-- 곡 목록 -->
        <div class="mb-6">
          <h2 class="text-sm font-semibold text-foreground mb-3">곡 순서</h2>
          <ol class="flex flex-col gap-2">
            <li
              v-for="(item, i) in setlist.items"
              :key="item.setlistItemId"
              class="flex items-center gap-3 text-sm"
            >
              <span class="w-5 h-5 rounded-full bg-muted text-muted-foreground text-xs flex items-center justify-center shrink-0 font-medium">{{ i + 1 }}</span>
              <span class="font-medium text-foreground">{{ item.songTitle }}</span>
              <span v-if="item.songArtist" class="text-muted-foreground text-xs">{{ item.songArtist }}</span>
              <span v-if="sheetLabel(item)" class="ml-auto text-xs text-muted-foreground shrink-0">{{ sheetLabel(item) }}</span>
            </li>
          </ol>
        </div>

        <p v-if="totalFileCount === 0" class="text-sm text-muted-foreground text-center py-8">등록된 악보 파일이 없습니다.</p>
      </template>
    </main>

    <SheetViewerModal
      v-if="showViewer"
      :songs="viewerSongs"
      :setlist-title="setlist?.title ?? setlist?.serviceDate ?? null"
      @close="showViewer = false"
    />
  </div>
</template>
