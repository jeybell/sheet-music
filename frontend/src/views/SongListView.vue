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
    <section class="page-heading">
      <p class="eyebrow">Songs</p>
      <h1>곡 목록</h1>
      <p class="description">등록된 찬양곡을 확인하고 상세 페이지로 이동할 수 있습니다.</p>
    </section>

    <section class="panel">
      <p v-if="songStore.isLoading" class="state-message">곡 목록을 불러오는 중입니다.</p>
      <p v-else-if="songStore.errorMessage" class="state-message error">
        {{ songStore.errorMessage }}
      </p>
      <p v-else-if="!songStore.hasSongs" class="state-message">등록된 곡이 없습니다.</p>
      <SongList v-else :songs="songStore.songs" />
    </section>
  </DefaultLayout>
</template>
