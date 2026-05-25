<script setup lang="ts">
import type { Song } from '../types/song'

defineProps<{
  songs: Song[]
}>()

const getSheetCount = (song: Song) => (song.sheets ?? song.songSheets ?? []).length
const getArtistName = (artist: string | null) => artist?.trim() || '아티스트 미상'
</script>

<template>
  <ul class="song-list">
    <li v-for="song in songs" :key="song.songId">
      <RouterLink :to="`/songs/${song.songId}`" class="song-link">{{ song.title }}</RouterLink>
      <span class="song-artist">{{ getArtistName(song.artist) }}</span>
      <span v-if="getSheetCount(song) > 0" class="song-sheet-count">
        악보 버전 {{ getSheetCount(song) }}개
      </span>
    </li>
  </ul>
</template>
