<script setup lang="ts">
import { Music } from '@lucide/vue'
import type { Song } from '../types/song'

defineProps<{
  songs: Song[]
}>()

const getSheets = (song: Song) => song.sheets ?? song.songSheets ?? []
const getSheetCount = (song: Song) => getSheets(song).length
const getFirstKey = (song: Song) => getSheets(song).find(s => s.sheetKey)?.sheetKey ?? null
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
    <RouterLink
      v-for="song in songs"
      :key="song.songId"
      :to="`/songs/${song.songId}`"
      class="bg-card rounded-xl border border-border p-3 sm:p-4 flex items-center gap-3 sm:gap-4 hover:border-primary/50 hover:shadow-md transition-all group"
    >
      <div class="shrink-0 w-10 h-10 sm:w-12 sm:h-12 rounded-lg bg-primary-soft flex items-center justify-center">
        <span
          v-if="getFirstKey(song)"
          class="text-sm font-bold text-primary leading-none"
        >{{ getFirstKey(song) }}</span>
        <Music v-else class="w-5 h-5 text-primary" />
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm font-semibold text-foreground group-hover:text-primary transition-colors truncate">
          {{ song.title }}
        </p>
        <p class="text-xs text-muted-foreground mt-0.5 truncate">
          {{ song.artist ?? '아티스트 미상' }}
        </p>
      </div>
      <span
        v-if="getSheetCount(song) > 0"
        class="shrink-0 inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground"
      >
        악보 {{ getSheetCount(song) }}
      </span>
    </RouterLink>
  </div>
</template>
