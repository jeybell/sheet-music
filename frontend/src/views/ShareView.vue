<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Music, ChevronLeft, ChevronRight } from '@lucide/vue'
import { getSharedSetlist } from '../apis/setlistApi'
import type { SharedSetlist, SharedSetlistItem, SharedFile } from '../types/setlist'

const route = useRoute()
const token = route.params.token as string

const setlist = ref<SharedSetlist | null>(null)
const isLoading = ref(true)
const errorMessage = ref('')

const apiBase = import.meta.env.VITE_API_BASE_URL || ''
const fileUrl = (fileId: number) => `${apiBase}/api/song-files/${fileId}/view`
const isPdf = (f: SharedFile) =>
  f.contentType?.includes('pdf') || f.originalFileName?.toLowerCase().endsWith('.pdf') || false

// ── 슬라이드 (곡 단위)
interface Slide { item: SharedSetlistItem; file: SharedFile; index: number }

const slides = computed<Slide[]>(() => {
  if (!setlist.value) return []
  return setlist.value.items.flatMap((item) =>
    item.files.map((file, index) => ({ item, file, index }))
  )
})

const currentSlide = ref(0)
const go = (delta: number) => {
  const len = slides.value.length
  if (len === 0) return
  currentSlide.value = (currentSlide.value + delta + len) % len
}

const sheetLabel = (item: SharedSetlistItem) => {
  if (item.sheetKey && item.versionName) return `${item.sheetKey} · ${item.versionName}`
  return item.sheetKey ?? item.versionName ?? null
}

const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${m}월 ${d}일`
}

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
        <div class="mb-6">
          <p class="text-xs text-muted-foreground mb-1">{{ formatDate(setlist.serviceDate) }}<span v-if="setlist.serviceType"> · {{ setlist.serviceType }}</span></p>
          <h1 class="text-xl font-bold text-foreground">{{ setlist.title ?? '콘티' }}</h1>
          <p v-if="setlist.memo" class="text-sm text-muted-foreground mt-1">{{ setlist.memo }}</p>
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

        <!-- 악보 슬라이드 뷰어 -->
        <div v-if="slides.length > 0">
          <h2 class="text-sm font-semibold text-foreground mb-3">악보 ({{ slides.length }}장)</h2>

          <!-- 슬라이드 썸네일 네비게이션 -->
          <div v-if="slides.length > 1" class="flex gap-1.5 mb-3 flex-wrap">
            <button
              v-for="(slide, i) in slides"
              :key="i"
              type="button"
              class="h-6 px-2 rounded text-xs font-medium transition-colors"
              :class="currentSlide === i ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground hover:bg-muted/80'"
              @click="currentSlide = i"
            >
              {{ slide.item.songTitle }}{{ slides.filter(s => s.item.setlistItemId === slide.item.setlistItemId).length > 1 ? ` ${slide.index + 1}` : '' }}
            </button>
          </div>

          <!-- 현재 슬라이드 -->
          <div class="relative bg-muted rounded-lg overflow-hidden">
            <div v-if="isPdf(slides[currentSlide].file)" class="py-10 text-center text-sm text-muted-foreground">
              PDF 파일은 미리보기를 지원하지 않습니다.
            </div>
            <img
              v-else
              :src="fileUrl(slides[currentSlide].file.songFileId)"
              :alt="slides[currentSlide].item.songTitle"
              class="w-full object-contain max-h-[70dvh]"
            />

            <!-- 이전/다음 -->
            <button
              v-if="slides.length > 1"
              type="button"
              class="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/40 flex items-center justify-center hover:bg-black/60 transition-colors"
              @click="go(-1)"
            >
              <ChevronLeft class="w-4 h-4 text-white" />
            </button>
            <button
              v-if="slides.length > 1"
              type="button"
              class="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/40 flex items-center justify-center hover:bg-black/60 transition-colors"
              @click="go(1)"
            >
              <ChevronRight class="w-4 h-4 text-white" />
            </button>

            <!-- 슬라이드 카운터 -->
            <div class="absolute bottom-2 left-1/2 -translate-x-1/2 bg-black/50 text-white text-xs px-2 py-0.5 rounded-full">
              {{ currentSlide + 1 }} / {{ slides.length }}
            </div>
          </div>
        </div>

        <p v-else class="text-sm text-muted-foreground text-center py-8">등록된 악보 파일이 없습니다.</p>
      </template>
    </main>
  </div>
</template>
