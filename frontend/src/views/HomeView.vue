<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Music, ChevronRight, CalendarDays, Flame, Star, History } from '@lucide/vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { getSetlists } from '../apis/setlistApi'
import { getPopularSongs } from '../apis/songApi'
import { useSetlistFavorites } from '../composables/useSetlistFavorites'
import type { Setlist } from '../types/setlist'
import type { PopularSong } from '../types/song'

const setlists = ref<Setlist[]>([])
const popularSongs = ref<PopularSong[]>([])
const isLoading = ref(true)
const { favoriteIds, recentIds } = useSetlistFavorites()

const daysUntil = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-').map(Number)
  const target = new Date(y, m - 1, d)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((target.getTime() - today.getTime()) / 86400000)
}

const ddayLabel = (dateStr: string) => {
  const diff = daysUntil(dateStr)
  if (diff === 0) return '오늘'
  if (diff === 1) return '내일'
  return `D-${diff}`
}

const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-')
  return `${y}.${m}.${d}`
}

// 가장 가까운 예정 콘티 (오늘 포함, 미래 중 가장 가까운 날짜)
const nextSetlist = computed(() => {
  return setlists.value
    .filter(s => daysUntil(s.serviceDate) >= 0)
    .sort((a, b) => a.serviceDate.localeCompare(b.serviceDate))[0] ?? null
})

// 최근 콘티 (serviceDate 내림차순, 최대 5개) — 다음 예정 콘티는 중복 제외
const recentSetlists = computed(() => {
  return setlists.value
    .filter(s => s.setlistId !== nextSetlist.value?.setlistId)
    .slice(0, 5)
})

// 즐겨찾기한 콘티 (브라우저 로컬 저장)
const favoriteSetlists = computed(() =>
  setlists.value.filter(s => favoriteIds.value.includes(s.setlistId)),
)

// 최근 열어본 콘티 (브라우저 로컬 저장, 열람 순서 유지)
const recentlyViewedSetlists = computed(() => {
  const byId = new Map(setlists.value.map(s => [s.setlistId, s]))
  return recentIds.value.map(id => byId.get(id)).filter((s): s is Setlist => !!s)
})

onMounted(async () => {
  try {
    const [sl, popular] = await Promise.all([getSetlists(), getPopularSongs(5)])
    setlists.value = sl
    popularSongs.value = popular
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <DefaultLayout>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold text-foreground">홈</h1>
      <RouterLink
        to="/setlists"
        class="inline-flex items-center gap-1.5 h-9 px-4 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:opacity-90 transition-opacity"
      >
        <Plus class="w-4 h-4" />
        새 콘티
      </RouterLink>
    </div>

    <p v-if="isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>

    <div v-else class="flex flex-col gap-6">
      <!-- 다음 예정 콘티 강조 -->
      <RouterLink
        v-if="nextSetlist"
        :to="`/setlists/${nextSetlist.setlistId}`"
        class="block rounded-xl border border-primary/40 bg-primary-soft/40 p-5 hover:border-primary transition-colors"
      >
        <div class="flex items-center justify-between gap-4">
          <div class="min-w-0">
            <p class="inline-flex items-center gap-1.5 text-xs font-medium text-primary mb-1.5">
              <CalendarDays class="w-3.5 h-3.5" />
              다음 예배
            </p>
            <p class="text-lg font-bold text-foreground truncate">
              {{ nextSetlist.title ?? '제목 없음' }}
            </p>
            <p class="text-sm text-muted-foreground mt-0.5">
              {{ formatDate(nextSetlist.serviceDate) }} · {{ nextSetlist.items.length }}곡
            </p>
          </div>
          <div class="shrink-0 text-right">
            <span class="text-2xl font-extrabold text-primary tabular-nums">{{ ddayLabel(nextSetlist.serviceDate) }}</span>
          </div>
        </div>
      </RouterLink>

      <div v-else class="rounded-xl border border-dashed border-border bg-muted/20 p-5 text-center">
        <p class="text-sm text-muted-foreground">예정된 콘티가 없습니다.</p>
        <RouterLink to="/setlists" class="text-sm text-primary font-medium hover:underline mt-1 inline-block">
          새 콘티 만들기 →
        </RouterLink>
      </div>

      <!-- 즐겨찾기 콘티 -->
      <section v-if="favoriteSetlists.length > 0">
        <h2 class="inline-flex items-center gap-1.5 text-sm font-semibold text-foreground mb-3">
          <Star class="w-4 h-4 text-primary fill-current" />
          즐겨찾기
        </h2>
        <div class="flex flex-col gap-2">
          <RouterLink
            v-for="setlist in favoriteSetlists"
            :key="setlist.setlistId"
            :to="`/setlists/${setlist.setlistId}`"
            class="flex items-center justify-between gap-3 bg-card rounded-lg border border-primary/30 px-4 py-3 hover:border-primary/50 transition-colors"
          >
            <div class="flex items-center gap-3 min-w-0">
              <span class="text-sm font-semibold text-foreground shrink-0">{{ formatDate(setlist.serviceDate) }}</span>
              <span v-if="setlist.title" class="text-sm text-muted-foreground truncate">{{ setlist.title }}</span>
            </div>
            <span class="text-xs text-muted-foreground shrink-0">{{ setlist.items.length }}곡</span>
          </RouterLink>
        </div>
      </section>

      <!-- 최근 열어본 콘티 -->
      <section v-if="recentlyViewedSetlists.length > 0">
        <h2 class="inline-flex items-center gap-1.5 text-sm font-semibold text-foreground mb-3">
          <History class="w-4 h-4 text-primary" />
          최근 열어본 콘티
        </h2>
        <div class="flex flex-col gap-2">
          <RouterLink
            v-for="setlist in recentlyViewedSetlists"
            :key="setlist.setlistId"
            :to="`/setlists/${setlist.setlistId}`"
            class="flex items-center justify-between gap-3 bg-card rounded-lg border border-border px-4 py-3 hover:border-primary/50 transition-colors"
          >
            <div class="flex items-center gap-3 min-w-0">
              <span class="text-sm font-semibold text-foreground shrink-0">{{ formatDate(setlist.serviceDate) }}</span>
              <span v-if="setlist.title" class="text-sm text-muted-foreground truncate">{{ setlist.title }}</span>
            </div>
            <span class="text-xs text-muted-foreground shrink-0">{{ setlist.items.length }}곡</span>
          </RouterLink>
        </div>
      </section>

      <!-- 최근 콘티 -->
      <section>
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-semibold text-foreground">최근 콘티</h2>
          <RouterLink to="/setlists" class="text-xs text-muted-foreground hover:text-foreground inline-flex items-center gap-0.5">
            전체 보기 <ChevronRight class="w-3.5 h-3.5" />
          </RouterLink>
        </div>
        <div v-if="recentSetlists.length > 0" class="flex flex-col gap-2">
          <RouterLink
            v-for="setlist in recentSetlists"
            :key="setlist.setlistId"
            :to="`/setlists/${setlist.setlistId}`"
            class="flex items-center justify-between gap-3 bg-card rounded-lg border border-border px-4 py-3 hover:border-primary/50 transition-colors"
          >
            <div class="flex items-center gap-3 min-w-0">
              <span class="text-sm font-semibold text-foreground shrink-0">{{ formatDate(setlist.serviceDate) }}</span>
              <span v-if="setlist.title" class="text-sm text-muted-foreground truncate">{{ setlist.title }}</span>
            </div>
            <span class="text-xs text-muted-foreground shrink-0">{{ setlist.items.length }}곡</span>
          </RouterLink>
        </div>
        <p v-else class="text-sm text-muted-foreground py-4 text-center">아직 콘티가 없습니다.</p>
      </section>

      <!-- 자주 쓰는 곡 Top 5 -->
      <section>
        <h2 class="inline-flex items-center gap-1.5 text-sm font-semibold text-foreground mb-3">
          <Flame class="w-4 h-4 text-primary" />
          자주 쓰는 곡
        </h2>
        <div v-if="popularSongs.length > 0" class="flex flex-col gap-2">
          <RouterLink
            v-for="(song, i) in popularSongs"
            :key="song.songId"
            :to="`/songs/${song.songId}`"
            class="flex items-center gap-3 bg-card rounded-lg border border-border px-4 py-3 hover:border-primary/50 transition-colors"
          >
            <span class="text-sm font-bold text-muted-foreground w-5 shrink-0 tabular-nums">{{ i + 1 }}</span>
            <Music class="w-4 h-4 text-muted-foreground shrink-0" />
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-foreground truncate">{{ song.title }}</p>
              <p v-if="song.artist" class="text-xs text-muted-foreground truncate">{{ song.artist }}</p>
            </div>
            <span class="text-xs text-muted-foreground shrink-0">{{ song.usageCount }}회</span>
          </RouterLink>
        </div>
        <p v-else class="text-sm text-muted-foreground py-4 text-center">콘티에 사용된 곡이 아직 없습니다.</p>
      </section>
    </div>
  </DefaultLayout>
</template>
