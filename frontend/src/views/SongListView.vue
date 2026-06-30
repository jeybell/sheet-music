<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Plus, Music, Search, X, FolderUp } from '@lucide/vue'
import SongList from '../components/SongList.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSongStore } from '../stores/songStore'
import { getAllTags } from '../apis/songApi'

const songStore = useSongStore()

const query = ref('')
const songKey = ref('')
const selectedTag = ref<string | null>(null)
const allTags = ref<string[]>([])
const hasFilter = ref(false)

let debounceTimer: ReturnType<typeof setTimeout> | undefined

const runSearch = () => {
  hasFilter.value = Boolean(query.value.trim() || songKey.value.trim() || selectedTag.value)
  void songStore.fetchSongs({ query: query.value, songKey: songKey.value, tag: selectedTag.value })
}

watch([query, songKey], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runSearch, 300)
})

watch(selectedTag, () => {
  runSearch()
})

const toggleTag = (tag: string) => {
  selectedTag.value = selectedTag.value === tag ? null : tag
}

const clearFilters = () => {
  query.value = ''
  songKey.value = ''
  selectedTag.value = null
}

onMounted(async () => {
  void songStore.fetchSongs()
  allTags.value = await getAllTags()
})
</script>

<template>
  <DefaultLayout>
    <div class="flex items-center justify-between mb-5">
      <h1 class="text-xl font-bold text-foreground">악보</h1>
      <div class="flex items-center gap-2">
        <RouterLink
          to="/songs/bulk"
          class="inline-flex items-center gap-1.5 h-9 px-4 rounded-md border border-border bg-card text-sm font-medium text-foreground hover:bg-muted transition-colors"
        >
          <FolderUp class="w-4 h-4" />
          일괄 업로드
        </RouterLink>
        <RouterLink
          to="/songs/new"
          class="inline-flex items-center gap-1.5 h-9 px-4 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:opacity-90 transition-opacity"
        >
          <Plus class="w-4 h-4" />
          곡 등록
        </RouterLink>
      </div>
    </div>

    <!-- 검색 영역 -->
    <div class="flex flex-col sm:flex-row gap-2 mb-6">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <input
          v-model="query"
          type="text"
          placeholder="제목 · 아티스트 · 가사 검색"
          class="w-full h-10 pl-9 pr-9 rounded-md border border-input bg-card text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent"
        />
        <button
          v-if="query"
          type="button"
          aria-label="검색어 지우기"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
          @click="query = ''"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
      <input
        v-model="songKey"
        type="text"
        placeholder="키 (예: C, G, Am)"
        class="h-10 w-full sm:w-40 px-3 rounded-md border border-input bg-card text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent"
      />
    </div>

    <!-- 태그 필터 -->
    <div v-if="allTags.length > 0" class="flex flex-wrap gap-1.5 mb-5">
      <button
        v-for="tag in allTags"
        :key="tag"
        type="button"
        class="inline-flex items-center h-7 px-2.5 rounded-full text-xs font-medium border transition-colors"
        :class="selectedTag === tag
          ? 'bg-primary text-primary-foreground border-primary'
          : 'bg-card text-muted-foreground border-border hover:border-primary hover:text-primary'"
        @click="toggleTag(tag)"
      >
        {{ tag }}
      </button>
    </div>

    <p v-if="songStore.isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="songStore.errorMessage" class="text-sm text-destructive py-4">{{ songStore.errorMessage }}</p>

    <!-- 검색 결과 없음 -->
    <div v-else-if="!songStore.hasSongs && hasFilter" class="py-16 flex flex-col items-center text-center">
      <div class="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-4">
        <Search class="w-6 h-6 text-muted-foreground" />
      </div>
      <p class="text-sm font-medium text-foreground">검색 결과가 없습니다</p>
      <p class="text-sm text-muted-foreground mt-1">다른 검색어나 키로 다시 시도해보세요.</p>
      <button
        type="button"
        class="mt-4 inline-flex items-center gap-1.5 h-9 px-4 rounded-md border border-border text-sm font-medium text-foreground hover:bg-muted transition-colors"
        @click="clearFilters"
      >
        <X class="w-4 h-4" />
        필터 초기화
      </button>
    </div>

    <!-- 곡 없음 -->
    <div v-else-if="!songStore.hasSongs" class="py-16 flex flex-col items-center text-center">
      <div class="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-4">
        <Music class="w-6 h-6 text-muted-foreground" />
      </div>
      <p class="text-sm font-medium text-foreground">등록된 곡이 없습니다</p>
      <p class="text-sm text-muted-foreground mt-1">첫 번째 곡을 등록해 악보를 정리해보세요.</p>
      <RouterLink
        to="/songs/new"
        class="mt-4 inline-flex items-center gap-1.5 h-9 px-4 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:opacity-90 transition-opacity"
      >
        <Plus class="w-4 h-4" />
        곡 등록
      </RouterLink>
    </div>

    <SongList v-else :songs="songStore.songs" />
  </DefaultLayout>
</template>
