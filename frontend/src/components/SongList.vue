<script setup lang="ts">
import { Music } from '@lucide/vue'
import type { Song } from '../types/song'

defineProps<{
  songs: Song[]
}>()

const getSheetCount = (song: Song) => (song.sheets ?? song.songSheets ?? []).length
</script>

<template>
  <div class="flex flex-col gap-2">
    <RouterLink
      v-for="song in songs"
      :key="song.songId"
      :to="`/songs/${song.songId}`"
      class="bg-white rounded-xl border border-zinc-200 px-5 py-4 flex items-center gap-4 hover:border-violet-300 hover:shadow-sm transition-all group"
    >
      <div class="flex-shrink-0 w-9 h-9 rounded-lg bg-violet-50 flex items-center justify-center">
        <Music class="w-4 h-4 text-violet-500" />
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm font-semibold text-zinc-900 group-hover:text-violet-700 transition-colors truncate">
          {{ song.title }}
        </p>
        <p class="text-xs text-zinc-400 mt-0.5 truncate">
          {{ song.artist ?? '아티스트 미상' }}
        </p>
      </div>
      <div v-if="getSheetCount(song) > 0" class="flex-shrink-0">
        <span class="inline-flex items-center rounded-full bg-zinc-100 px-2.5 py-0.5 text-xs font-medium text-zinc-500">
          악보 {{ getSheetCount(song) }}
        </span>
      </div>
    </RouterLink>
  </div>
</template>
