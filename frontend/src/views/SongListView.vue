<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Plus, Music, Search, X, FolderUp, Loader2 } from '@lucide/vue'
import SongList from '../components/SongList.vue'
import Select from '../components/ui/Select.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSongStore } from '../stores/songStore'
import { getAllTags } from '../apis/songApi'

const songStore = useSongStore()

const query = ref('')
const songKey = ref('')
const selectedTags = ref<string[]>([])
const allTags = ref<string[]>([])
const hasFilter = ref(false)

const SORT_OPTIONS = [
  { value: 'TITLE', label: '이름순' },
  { value: 'ARTIST', label: '아티스트순' },
  { value: 'KEY', label: '키순' },
  { value: 'LATEST', label: '최신순' },
] as const
const SORT_STORAGE_KEY = 'song-list-sort'
const sortBy = ref<string>(localStorage.getItem(SORT_STORAGE_KEY) ?? 'TITLE')

const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | undefined

let debounceTimer: ReturnType<typeof setTimeout> | undefined

const runSearch = () => {
  hasFilter.value = Boolean(query.value.trim() || songKey.value.trim() || selectedTags.value.length)
  void songStore.fetchFirstPage({
    query: query.value,
    songKey: songKey.value,
    tags: selectedTags.value,
    sort: sortBy.value,
  })
}

watch(sortBy, (value) => {
  localStorage.setItem(SORT_STORAGE_KEY, value)
  runSearch()
})

watch([query, songKey], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runSearch, 300)
})

watch(selectedTags, () => {
  runSearch()
}, { deep: true })

const toggleTag = (tag: string) => {
  const idx = selectedTags.value.indexOf(tag)
  if (idx === -1) selectedTags.value.push(tag)
  else selectedTags.value.splice(idx, 1)
}

const clearTags = () => {
  selectedTags.value = []
}

const clearFilters = () => {
  query.value = ''
  songKey.value = ''
  selectedTags.value = []
}

// sentinel 은 목록이 렌더된 뒤에야 존재하므로 ref 변화를 감지해 관찰을 건다.
watch(sentinel, (el, prev) => {
  if (prev && observer) observer.unobserve(prev)
  if (!el) return
  if (!observer) {
    observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting) void songStore.fetchNextPage()
    }, { rootMargin: '200px' })
  }
  observer.observe(el)
})

onMounted(async () => {
  runSearch()
  allTags.value = await getAllTags()
})

onBeforeUnmount(() => observer?.disconnect())
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

    <!-- 태그 필터 (다중 선택, AND) -->
    <div v-if="allTags.length > 0" class="flex flex-wrap items-center gap-1.5 mb-5">
      <button
        v-for="tag in allTags"
        :key="tag"
        type="button"
        class="inline-flex items-center h-7 px-2.5 rounded-full text-xs font-medium border transition-colors"
        :class="selectedTags.includes(tag)
          ? 'bg-primary text-primary-foreground border-primary'
          : 'bg-card text-muted-foreground border-border hover:border-primary hover:text-primary'"
        @click="toggleTag(tag)"
      >
        {{ tag }}
      </button>
      <button
        v-if="selectedTags.length > 0"
        type="button"
        class="inline-flex items-center gap-1 h-7 px-2.5 rounded-full text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
        @click="clearTags"
      >
        <X class="w-3 h-3" />
        태그 {{ selectedTags.length }}개 해제
      </button>
    </div>

    <!-- 결과 건수 + 정렬 -->
    <div class="flex items-center justify-between gap-2 mb-3">
      <p class="text-xs text-muted-foreground">
        <template v-if="songStore.hasListSongs">
          {{ hasFilter ? '검색 결과' : '전체' }} 총 <span class="font-medium text-foreground">{{ songStore.listTotal }}</span>곡
        </template>
      </p>
      <div class="flex items-center gap-1.5 shrink-0">
        <span class="text-xs text-muted-foreground">정렬</span>
        <Select v-model="sortBy" class="!h-8 !w-28 text-xs">
          <option v-for="opt in SORT_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </Select>
      </div>
    </div>

    <p v-if="songStore.isLoadingList" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="songStore.errorMessage" class="text-sm text-destructive py-4">{{ songStore.errorMessage }}</p>

    <!-- 검색 결과 없음 -->
    <div v-else-if="!songStore.hasListSongs && hasFilter" class="py-16 flex flex-col items-center text-center">
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
    <div v-else-if="!songStore.hasListSongs" class="py-16 flex flex-col items-center text-center">
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

    <template v-else>
      <SongList :songs="songStore.listSongs" />

      <!-- 무한 스크롤 감지 지점 -->
      <div ref="sentinel" class="h-4" />
      <div v-if="songStore.isLoadingMore" class="flex items-center justify-center gap-2 py-4 text-sm text-muted-foreground">
        <Loader2 class="w-4 h-4 animate-spin" />
        더 불러오는 중...
      </div>
    </template>
  </DefaultLayout>
</template>
