<script setup lang="ts">
import type { Song } from '../apis/songs'

defineProps<{
  songs: Song[]
}>()
</script>

<template>
  <ul class="song-list">
    <li v-for="song in songs" :key="song.id">
      <RouterLink :to="`/songs/${song.id}`" class="song-card">
        <span class="song-title">{{ song.title }}</span>
        <span class="song-artist">{{ song.artist || '아티스트 미상' }}</span>
      </RouterLink>
    </li>
  </ul>
</template>
