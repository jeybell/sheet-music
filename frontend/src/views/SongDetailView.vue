<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSongStore } from '../stores/songStore'
import type { SongFile, SongSheetSummary } from '../types/song'

const props = defineProps<{
  songId: number
}>()

const songStore = useSongStore()

const song = computed(() => songStore.selectedSong)
const sheets = computed(() => song.value?.sheets ?? song.value?.songSheets ?? [])
const songFiles = computed(() => song.value?.files ?? [])
const hasSongFilesField = computed(() => song.value?.files !== undefined)

const getDisplayText = (value: string | null | undefined, fallback: string) =>
  value?.trim() || fallback

const getSheetId = (sheet: SongSheetSummary) => sheet.id ?? sheet.songSheetId

const getFileName = (file: SongFile) =>
  getDisplayText(file.originalFileName ?? file.storedFileName ?? file.filePath, '파일명 없음')

const loadSong = () => {
  if (Number.isFinite(props.songId)) {
    void songStore.fetchSong(props.songId)
  }
}

onMounted(loadSong)
watch(() => props.songId, loadSong)
</script>

<template>
  <DefaultLayout>
    <button type="button" @click="$router.push('/songs')">목록으로 돌아가기</button>

    <section>
      <p v-if="songStore.isLoading">곡 정보를 불러오는 중입니다.</p>
      <p v-else-if="songStore.errorMessage" class="error">
        {{ songStore.errorMessage }}
      </p>
      <p v-else-if="!song">곡 정보를 찾을 수 없습니다.</p>
      <article v-else>
        <h1>{{ song.title }}</h1>
        <p>아티스트: {{ getDisplayText(song.artist, '아티스트 미상') }}</p>
        <p>작곡가: {{ getDisplayText(song.composer, '작곡가 미상') }}</p>
        <p>메모: {{ getDisplayText(song.memo, '메모 없음') }}</p>

        <section>
          <h2>악보</h2>
          <p v-if="sheets.length === 0">등록된 악보가 없습니다.</p>
          <ul v-else>
            <li v-for="(sheet, index) in sheets" :key="getSheetId(sheet) ?? index">
              <p>키: {{ getDisplayText(sheet.sheetKey, '키 미지정') }}</p>
              <p>버전: {{ getDisplayText(sheet.versionName, '버전명 없음') }}</p>
              <p v-if="sheet.memo">메모: {{ sheet.memo }}</p>

              <p v-if="!sheet.files?.length">등록된 파일 없음</p>
              <ul v-else>
                <li v-for="file in sheet.files" :key="file.id">
                  {{ getFileName(file) }}
                </li>
              </ul>
            </li>
          </ul>
        </section>

        <section v-if="hasSongFilesField">
          <h2>파일</h2>
          <p v-if="songFiles.length === 0">등록된 파일 없음</p>
          <ul v-else>
            <li v-for="file in songFiles" :key="file.id">
              {{ getFileName(file) }}
            </li>
          </ul>
        </section>
      </article>
    </section>
  </DefaultLayout>
</template>
