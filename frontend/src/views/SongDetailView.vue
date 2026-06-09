<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { deleteSong, updateSong } from '../apis/songApi'
import { deleteSongFile, uploadSongSheetFile } from '../apis/songFileApi'
import { createSongSheet, deleteSongSheet } from '../apis/songSheetApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSongStore } from '../stores/songStore'
import type { SongSheetSummary } from '../types/song'

interface ApiErrorResponse {
  message?: string
}

const props = defineProps<{ songId: number }>()
const router = useRouter()
const songStore = useSongStore()

const song = computed(() => songStore.selectedSong)
const sheets = computed(() => song.value?.sheets ?? song.value?.songSheets ?? [])

const apiError = (error: unknown, fallback: string) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return fallback
}

const toOpt = (v: string) => v.trim() || null

// ── 곡 수정 ──────────────────────────────────────────────────
const isEditing = ref(false)
const editForm = reactive({ title: '', artist: '', composer: '', memo: '' })
const editError = ref('')
const isSavingEdit = ref(false)

const startEdit = () => {
  editForm.title = song.value?.title ?? ''
  editForm.artist = song.value?.artist ?? ''
  editForm.composer = song.value?.composer ?? ''
  editForm.memo = song.value?.memo ?? ''
  editError.value = ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editError.value = ''
}

const handleUpdateSong = async () => {
  if (!editForm.title.trim()) {
    editError.value = '제목은 필수입니다.'
    return
  }
  isSavingEdit.value = true
  editError.value = ''
  try {
    await updateSong(props.songId, {
      title: editForm.title.trim(),
      artist: toOpt(editForm.artist),
      composer: toOpt(editForm.composer),
      memo: toOpt(editForm.memo),
    })
    await songStore.fetchSong(props.songId)
    isEditing.value = false
  } catch (e) {
    editError.value = apiError(e, '수정에 실패했습니다.')
  } finally {
    isSavingEdit.value = false
  }
}

// ── 곡 삭제 ──────────────────────────────────────────────────
const handleDeleteSong = async () => {
  if (!confirm(`"${song.value?.title}" 곡을 삭제할까요?`)) return
  try {
    await deleteSong(props.songId)
    await router.push('/songs')
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 악보 버전 추가 ─────────────────────────────────────────
const showAddSheet = ref(false)
const sheetForm = reactive({ sheetKey: '', versionName: '', memo: '' })
const sheetError = ref('')
const sheetMessage = ref('')
const isCreatingSheet = ref(false)

const resetSheetForm = () => {
  sheetForm.sheetKey = ''
  sheetForm.versionName = ''
  sheetForm.memo = ''
  sheetError.value = ''
  sheetMessage.value = ''
}

const handleCreateSheet = async () => {
  sheetError.value = ''
  sheetMessage.value = ''
  isCreatingSheet.value = true
  try {
    await createSongSheet(props.songId, {
      sheetKey: toOpt(sheetForm.sheetKey),
      versionName: toOpt(sheetForm.versionName),
      memo: toOpt(sheetForm.memo),
    })
    resetSheetForm()
    showAddSheet.value = false
    await songStore.fetchSong(props.songId)
  } catch (e) {
    sheetError.value = apiError(e, '악보 버전 추가에 실패했습니다.')
  } finally {
    isCreatingSheet.value = false
  }
}

// ── 악보 버전 삭제 ─────────────────────────────────────────
const handleDeleteSheet = async (sheet: SongSheetSummary) => {
  const label = sheet.versionName || sheet.sheetKey || '이 버전'
  if (!confirm(`"${label}"을 삭제할까요?`)) return
  try {
    await deleteSongSheet(sheet.songSheetId)
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 파일 업로드 ────────────────────────────────────────────
const selectedFiles = ref<Record<number, File | undefined>>({})
const uploadInputKeys = ref<Record<number, number>>({})
const uploadMessages = ref<Record<number, string | undefined>>({})
const uploadErrors = ref<Record<number, string | undefined>>({})
const uploadingSheets = ref<Record<number, boolean>>({})

const handleFileChange = (event: Event, sheetId: number) => {
  const input = event.target as HTMLInputElement
  selectedFiles.value[sheetId] = input.files?.[0]
  uploadMessages.value[sheetId] = undefined
  uploadErrors.value[sheetId] = undefined
}

const handleUpload = async (sheetId: number) => {
  const file = selectedFiles.value[sheetId]
  if (!file) {
    uploadMessages.value[sheetId] = '파일을 선택해주세요.'
    return
  }
  uploadingSheets.value[sheetId] = true
  uploadErrors.value[sheetId] = undefined
  uploadMessages.value[sheetId] = undefined
  try {
    await uploadSongSheetFile(sheetId, file)
    selectedFiles.value[sheetId] = undefined
    uploadInputKeys.value[sheetId] = (uploadInputKeys.value[sheetId] ?? 0) + 1
    uploadMessages.value[sheetId] = '업로드 완료'
    await songStore.fetchSong(props.songId)
  } catch (e) {
    uploadErrors.value[sheetId] = apiError(e, '업로드에 실패했습니다.')
  } finally {
    uploadingSheets.value[sheetId] = false
  }
}

// ── 파일 삭제 ──────────────────────────────────────────────
const handleDeleteFile = async (fileId: number, fileName: string) => {
  if (!confirm(`"${fileName}" 파일을 삭제할까요?`)) return
  try {
    await deleteSongFile(fileId)
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '파일 삭제에 실패했습니다.'))
  }
}

// ── 초기 로드 ──────────────────────────────────────────────
const loadSong = () => {
  if (Number.isFinite(props.songId)) void songStore.fetchSong(props.songId)
}

onMounted(loadSong)
watch(() => props.songId, loadSong)
</script>

<template>
  <DefaultLayout>
    <div class="detail-back">
      <button type="button" class="btn-back" @click="$router.push('/songs')">
        ← 목록으로
      </button>
    </div>

    <p v-if="songStore.isLoading" class="text-muted">불러오는 중...</p>
    <p v-else-if="songStore.errorMessage" class="error">{{ songStore.errorMessage }}</p>

    <template v-else-if="song">
      <!-- 곡 정보 -->
      <section class="detail-card">
        <template v-if="!isEditing">
          <div class="detail-header">
            <h1 class="detail-title">{{ song.title }}</h1>
            <div class="detail-actions">
              <button type="button" @click="startEdit">수정</button>
              <button type="button" class="btn-danger" @click="handleDeleteSong">삭제</button>
            </div>
          </div>
          <dl class="detail-meta">
            <template v-if="song.artist">
              <dt>아티스트</dt>
              <dd>{{ song.artist }}</dd>
            </template>
            <template v-if="song.composer">
              <dt>작곡가</dt>
              <dd>{{ song.composer }}</dd>
            </template>
            <template v-if="song.memo">
              <dt>메모</dt>
              <dd class="song-memo">{{ song.memo }}</dd>
            </template>
          </dl>
        </template>

        <template v-else>
          <h2 style="margin-top:0">곡 수정</h2>
          <p v-if="editError" class="error">{{ editError }}</p>
          <div class="form-field">
            <label for="edit-title">제목 *</label>
            <input id="edit-title" v-model="editForm.title" type="text" />
          </div>
          <div class="form-field">
            <label for="edit-artist">아티스트</label>
            <input id="edit-artist" v-model="editForm.artist" type="text" />
          </div>
          <div class="form-field">
            <label for="edit-composer">작곡가</label>
            <input id="edit-composer" v-model="editForm.composer" type="text" />
          </div>
          <div class="form-field">
            <label for="edit-memo">메모</label>
            <textarea id="edit-memo" v-model="editForm.memo" rows="3" />
          </div>
          <button type="button" class="btn-primary" :disabled="isSavingEdit" @click="handleUpdateSong">
            {{ isSavingEdit ? '저장 중...' : '저장' }}
          </button>
          <button type="button" :disabled="isSavingEdit" @click="cancelEdit">취소</button>
        </template>
      </section>

      <!-- 악보 버전 -->
      <section>
        <div class="section-header">
          <h2 style="margin:0">악보 버전</h2>
          <button type="button" @click="showAddSheet = !showAddSheet">
            {{ showAddSheet ? '취소' : '+ 버전 추가' }}
          </button>
        </div>

        <!-- 버전 추가 폼 -->
        <div v-if="showAddSheet" class="add-sheet-form">
          <p v-if="sheetError" class="error">{{ sheetError }}</p>
          <div class="form-row">
            <div class="form-field">
              <label for="sheet-key">키</label>
              <input id="sheet-key" v-model="sheetForm.sheetKey" type="text" placeholder="예) C, G" />
            </div>
            <div class="form-field">
              <label for="version-name">버전명</label>
              <input id="version-name" v-model="sheetForm.versionName" type="text" placeholder="예) 원본, 남성용" />
            </div>
          </div>
          <div class="form-field">
            <label for="sheet-memo">메모</label>
            <textarea id="sheet-memo" v-model="sheetForm.memo" rows="2" />
          </div>
          <button type="button" class="btn-primary" :disabled="isCreatingSheet" @click="handleCreateSheet">
            {{ isCreatingSheet ? '추가 중...' : '추가' }}
          </button>
        </div>

        <!-- 버전 목록 -->
        <p v-if="sheets.length === 0" class="text-muted">등록된 악보 버전이 없습니다.</p>
        <ul v-else class="sheet-list">
          <li v-for="sheet in sheets" :key="sheet.songSheetId" class="sheet-item">
            <div class="sheet-header">
              <div class="sheet-label">
                <span v-if="sheet.sheetKey" class="sheet-key">{{ sheet.sheetKey }}</span>
                <span v-if="sheet.versionName" class="sheet-version">{{ sheet.versionName }}</span>
                <span v-if="!sheet.sheetKey && !sheet.versionName" class="text-muted">버전명 없음</span>
              </div>
              <button type="button" class="btn-danger btn-sm" @click="handleDeleteSheet(sheet)">삭제</button>
            </div>
            <p v-if="sheet.memo" class="sheet-memo text-muted">{{ sheet.memo }}</p>

            <!-- 파일 목록 -->
            <ul v-if="sheet.files?.length" class="file-list">
              <li v-for="file in sheet.files" :key="file.songFileId" class="file-item">
                <span class="file-name">{{ file.originalFileName ?? file.storedFileName ?? '파일명 없음' }}</span>
                <button
                  type="button"
                  class="btn-danger btn-sm"
                  @click="handleDeleteFile(file.songFileId, file.originalFileName ?? '파일')"
                >삭제</button>
              </li>
            </ul>
            <p v-else class="text-muted" style="font-size:13px; margin: 6px 0">파일 없음</p>

            <!-- 파일 업로드 -->
            <div class="upload-row">
              <input
                :key="`${sheet.songSheetId}-${uploadInputKeys[sheet.songSheetId] ?? 0}`"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg"
                @change="handleFileChange($event, sheet.songSheetId)"
              />
              <button
                type="button"
                :disabled="uploadingSheets[sheet.songSheetId]"
                @click="handleUpload(sheet.songSheetId)"
              >
                {{ uploadingSheets[sheet.songSheetId] ? '업로드 중...' : '업로드' }}
              </button>
              <span v-if="uploadMessages[sheet.songSheetId]" class="success">{{ uploadMessages[sheet.songSheetId] }}</span>
              <span v-if="uploadErrors[sheet.songSheetId]" class="error">{{ uploadErrors[sheet.songSheetId] }}</span>
            </div>
          </li>
        </ul>
      </section>
    </template>
  </DefaultLayout>
</template>

<style scoped>
.detail-back {
  margin-bottom: 20px;
}

.btn-back {
  background: none;
  border: none;
  padding: 0;
  color: #1971c2;
  font-size: 14px;
  cursor: pointer;
  margin-left: 0;
}

.btn-back:hover {
  text-decoration: underline;
}

.detail-card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 32px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-title {
  margin: 0;
  font-size: 24px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.detail-meta {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 6px 12px;
  margin: 0;
  font-size: 14px;
}

.detail-meta dt {
  color: #868e96;
  font-weight: 500;
}

.detail-meta dd {
  margin: 0;
  color: #1a1a2e;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.add-sheet-form {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.sheet-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sheet-item {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.sheet-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sheet-key {
  background: #e8f4fd;
  color: #1971c2;
  font-size: 13px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.sheet-version {
  font-size: 14px;
  font-weight: 500;
  color: #343a40;
}

.sheet-memo {
  margin: 0 0 10px;
  font-size: 13px;
}

.file-list {
  list-style: none;
  padding: 0;
  margin: 0 0 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
  border-bottom: 1px solid #f1f3f5;
}

.file-name {
  color: #495057;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.upload-row input[type="file"] {
  width: auto;
  font-size: 13px;
  padding: 4px;
  border: 1px solid #ced4da;
  border-radius: 4px;
}

.btn-primary {
  background: #1971c2;
  color: #fff;
  border-color: #1971c2;
}

.btn-primary:hover:not(:disabled) {
  background: #1864ab;
}

.btn-danger {
  background: #fff;
  color: #c92a2a;
  border-color: #ffc9c9;
}

.btn-danger:hover:not(:disabled) {
  background: #fff5f5;
}

.btn-sm {
  padding: 4px 10px;
  font-size: 12px;
}
</style>
