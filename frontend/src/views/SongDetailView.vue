<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSongStore } from '../stores/songStore'

const props = defineProps<{
  id: number
}>()

const songStore = useSongStore()

const loadSong = () => {
  if (Number.isFinite(props.id)) {
    void songStore.fetchSong(props.id)
  }
}

onMounted(loadSong)
watch(() => props.id, loadSong)
</script>

<template>
  <DefaultLayout>
    <RouterLink to="/songs" class="back-link">목록으로 돌아가기</RouterLink>

    <section class="panel detail-panel">
      <p v-if="songStore.isLoading" class="state-message">곡 정보를 불러오는 중입니다.</p>
      <p v-else-if="songStore.errorMessage" class="state-message error">
        {{ songStore.errorMessage }}
      </p>
      <article v-else-if="songStore.selectedSong" class="song-detail">
        <p class="eyebrow">Song Detail</p>
        <h1>{{ songStore.selectedSong.title }}</h1>
        <p class="detail-artist">{{ songStore.selectedSong.artist || '아티스트 미상' }}</p>
        <p v-if="songStore.selectedSong.memo" class="detail-memo">
          {{ songStore.selectedSong.memo }}
        </p>
      </article>
    </section>
  </DefaultLayout>
</template>
