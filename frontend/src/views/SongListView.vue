<script setup lang="ts">
import { onMounted } from 'vue'
import { Plus } from '@lucide/vue'
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
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold text-zinc-900">곡 목록</h1>
      <RouterLink
        to="/songs/new"
        class="inline-flex items-center gap-1.5 h-9 px-4 rounded-md bg-violet-600 text-white text-sm font-medium hover:bg-violet-700 transition-colors"
      >
        <Plus class="w-4 h-4" />
        곡 등록
      </RouterLink>
    </div>

    <p v-if="songStore.isLoading" class="text-sm text-zinc-400 py-8 text-center">불러오는 중...</p>
    <p v-else-if="songStore.errorMessage" class="text-sm text-red-500 py-4">{{ songStore.errorMessage }}</p>
    <div v-else-if="!songStore.hasSongs" class="py-16 text-center">
      <p class="text-sm text-zinc-400">등록된 곡이 없습니다.</p>
      <RouterLink to="/songs/new" class="mt-3 inline-flex text-sm text-violet-600 hover:underline">
        첫 번째 곡을 등록해보세요
      </RouterLink>
    </div>
    <SongList v-else :songs="songStore.songs" />
  </DefaultLayout>
</template>
