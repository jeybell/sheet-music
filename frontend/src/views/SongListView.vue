<script setup lang="ts">
import { onMounted } from 'vue'
import SongList from '../components/SongList.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSongStore } from '../stores/songStore'

const songStore = useSongStore()

onMounted(() => {
  void songStore.fetchSongs()
})
</script>

<template>
  <DefaultLayout>
    <h1>곡 목록</h1>

    <section>
      <p v-if="songStore.isLoading">불러오는 중...</p>
      <p v-else-if="songStore.errorMessage" class="error">
        {{ songStore.errorMessage }}
      </p>
      <p v-else-if="!songStore.hasSongs">등록된 곡이 없습니다.</p>
      <SongList v-else :songs="songStore.songs" />
    </section>
  </DefaultLayout>
</template>
