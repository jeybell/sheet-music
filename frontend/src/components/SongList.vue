<script setup lang="ts">
import { Music, Check } from '@lucide/vue'
import { RouterLink } from 'vue-router'
import type { Song } from '../types/song'

const props = defineProps<{
  songs: Song[]
  selectable?: boolean
  selectedIds?: number[]
}>()

const emit = defineEmits<{ (e: 'toggle', songId: number): void }>()

const getSheets = (song: Song) => song.sheets ?? song.songSheets ?? []
const getSheetCount = (song: Song) => getSheets(song).length
const getFirstKey = (song: Song) => getSheets(song).find(s => s.sheetKey)?.sheetKey ?? null

const isSelected = (songId: number) => props.selectedIds?.includes(songId) ?? false
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
    <component
      :is="selectable ? 'button' : RouterLink"
      v-for="song in songs"
      :key="song.songId"
      :type="selectable ? 'button' : undefined"
      :to="selectable ? undefined : `/songs/${song.songId}`"
      class="bg-card rounded-xl border p-3 sm:p-4 flex items-center gap-3 sm:gap-4 transition-all group"
      :class="selectable
        ? (isSelected(song.songId)
            ? 'text-left border-primary ring-1 ring-primary/40 bg-primary-soft/30'
            : 'text-left border-border hover:border-primary/50')
        : 'border-border hover:border-primary/50 hover:shadow-md'"
      @click="selectable ? emit('toggle', song.songId) : undefined"
    >
      <!-- 선택 모드: 체크박스 / 일반 모드: 키 배지 -->
      <div
        v-if="selectable"
        class="shrink-0 w-5 h-5 rounded border flex items-center justify-center transition-colors"
        :class="isSelected(song.songId) ? 'bg-primary border-primary text-primary-foreground' : 'border-muted-foreground/40'"
      >
        <Check v-if="isSelected(song.songId)" class="w-3.5 h-3.5" />
      </div>
      <div v-else class="shrink-0 w-10 h-10 sm:w-12 sm:h-12 rounded-lg bg-primary-soft flex items-center justify-center">
        <span v-if="getFirstKey(song)" class="text-sm font-bold text-primary leading-none">{{ getFirstKey(song) }}</span>
        <Music v-else class="w-5 h-5 text-primary" />
      </div>

      <div class="flex-1 min-w-0">
        <p
          class="text-sm font-semibold text-foreground truncate transition-colors"
          :class="{ 'group-hover:text-primary': !selectable }"
        >{{ song.title }}</p>
        <p class="text-xs text-muted-foreground mt-0.5 truncate">{{ song.artist ?? '아티스트 미상' }}</p>
      </div>
      <span
        v-if="getSheetCount(song) > 0"
        class="shrink-0 inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground"
      >
        악보 {{ getSheetCount(song) }}
      </span>
    </component>
  </div>
</template>
