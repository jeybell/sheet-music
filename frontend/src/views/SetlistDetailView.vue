<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { deleteSetlist, updateSetlist } from '../apis/setlistApi'
import { addSetlistItem, deleteSetlistItem } from '../apis/setlistItemApi'
import { getSong } from '../apis/songApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useSetlistStore } from '../stores/setlistStore'
import { useSongStore } from '../stores/songStore'
import type { SongSheetSummary } from '../types/song'

interface ApiErrorResponse { message?: string }

const props = defineProps<{ setlistId: number }>()
const router = useRouter()
const store = useSetlistStore()
const songStore = useSongStore()

const setlist = computed(() => store.selectedSetlist)
const items = computed(() => [...(setlist.value?.items ?? [])].sort((a, b) => a.orderNo - b.orderNo))

const apiError = (e: unknown, fallback: string) =>
  isAxiosError<ApiErrorResponse>(e) ? (e.response?.data?.message ?? fallback) : fallback

// ── 셋리스트 수정 ─────────────────────────────────────────
const isEditing = ref(false)
const editForm = reactive({ serviceDate: '', serviceType: '', title: '', memo: '' })
const editError = ref('')
const isSavingEdit = ref(false)

const startEdit = () => {
  editForm.serviceDate = setlist.value?.serviceDate ?? ''
  editForm.serviceType = setlist.value?.serviceType ?? ''
  editForm.title = setlist.value?.title ?? ''
  editForm.memo = setlist.value?.memo ?? ''
  editError.value = ''
  isEditing.value = true
}

const handleUpdate = async () => {
  if (!editForm.serviceDate) {
    editError.value = '날짜는 필수입니다.'
    return
  }
  isSavingEdit.value = true
  editError.value = ''
  try {
    await updateSetlist(props.setlistId, {
      serviceDate: editForm.serviceDate,
      serviceType: editForm.serviceType.trim() || null,
      title: editForm.title.trim() || null,
      memo: editForm.memo.trim() || null,
    })
    await store.fetchSetlist(props.setlistId)
    isEditing.value = false
  } catch (e) {
    editError.value = apiError(e, '수정에 실패했습니다.')
  } finally {
    isSavingEdit.value = false
  }
}

// ── 셋리스트 삭제 ─────────────────────────────────────────
const handleDelete = async () => {
  const label = setlist.value?.title ?? setlist.value?.serviceDate ?? '이 셋리스트'
  if (!confirm(`"${label}"을 삭제할까요?`)) return
  try {
    await deleteSetlist(props.setlistId)
    await router.push('/setlists')
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 곡 추가 ───────────────────────────────────────────────
const showAddItem = ref(false)
const addForm = reactive({ songId: null as number | null, songSheetId: null as number | null, memo: '' })
const addError = ref('')
const isAddingItem = ref(false)
const availableSheets = ref<SongSheetSummary[]>([])
const isLoadingSheets = ref(false)

watch(() => addForm.songId, async (songId) => {
  addForm.songSheetId = null
  availableSheets.value = []
  if (!songId) return
  isLoadingSheets.value = true
  try {
    const song = await getSong(songId)
    availableSheets.value = song.sheets ?? song.songSheets ?? []
  } finally {
    isLoadingSheets.value = false
  }
})

const resetAddForm = () => {
  addForm.songId = null
  addForm.songSheetId = null
  addForm.memo = ''
  addError.value = ''
  availableSheets.value = []
}

const handleAddItem = async () => {
  if (!addForm.songId) {
    addError.value = '곡을 선택해주세요.'
    return
  }
  isAddingItem.value = true
  addError.value = ''
  try {
    await addSetlistItem(props.setlistId, {
      songId: addForm.songId,
      songSheetId: addForm.songSheetId ?? undefined,
      orderNo: items.value.length + 1,
      memo: addForm.memo.trim() || null,
    })
    resetAddForm()
    showAddItem.value = false
    await store.fetchSetlist(props.setlistId)
  } catch (e) {
    addError.value = apiError(e, '곡 추가에 실패했습니다.')
  } finally {
    isAddingItem.value = false
  }
}

// ── 곡 삭제 ───────────────────────────────────────────────
const handleDeleteItem = async (itemId: number, songTitle: string) => {
  if (!confirm(`"${songTitle}"을 셋리스트에서 제거할까요?`)) return
  try {
    await deleteSetlistItem(itemId)
    await store.fetchSetlist(props.setlistId)
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${m}월 ${d}일`
}

const sheetLabel = (key: string | null, version: string | null) => {
  if (key && version) return `${key} · ${version}`
  return key ?? version ?? null
}

const load = () => {
  if (Number.isFinite(props.setlistId)) {
    void store.fetchSetlist(props.setlistId)
    void songStore.fetchSongs()
  }
}

onMounted(load)
watch(() => props.setlistId, load)
</script>

<template>
  <DefaultLayout>
    <div class="detail-back">
      <button type="button" class="btn-back" @click="$router.push('/setlists')">
        ← 목록으로
      </button>
    </div>

    <p v-if="store.isLoading" class="text-muted">불러오는 중...</p>
    <p v-else-if="store.errorMessage" class="error">{{ store.errorMessage }}</p>

    <template v-else-if="setlist">
      <!-- 셋리스트 정보 -->
      <section class="detail-card">
        <template v-if="!isEditing">
          <div class="detail-header">
            <div>
              <div class="setlist-meta-row">
                <span class="setlist-date">{{ formatDate(setlist.serviceDate) }}</span>
                <span v-if="setlist.serviceType" class="setlist-type">{{ setlist.serviceType }}</span>
              </div>
              <h1 class="detail-title">{{ setlist.title ?? '제목 없음' }}</h1>
            </div>
            <div class="detail-actions">
              <button type="button" @click="startEdit">수정</button>
              <button type="button" class="btn-danger" @click="handleDelete">삭제</button>
            </div>
          </div>
          <p v-if="setlist.memo" class="setlist-memo text-muted">{{ setlist.memo }}</p>
        </template>

        <template v-else>
          <h2 style="margin-top:0">셋리스트 수정</h2>
          <p v-if="editError" class="error">{{ editError }}</p>
          <div class="form-row">
            <div class="form-field">
              <label for="edit-date">날짜 *</label>
              <input id="edit-date" v-model="editForm.serviceDate" type="date" />
            </div>
            <div class="form-field">
              <label for="edit-type">예배 종류</label>
              <input id="edit-type" v-model="editForm.serviceType" type="text" />
            </div>
          </div>
          <div class="form-field">
            <label for="edit-title">제목</label>
            <input id="edit-title" v-model="editForm.title" type="text" />
          </div>
          <div class="form-field">
            <label for="edit-memo">메모</label>
            <textarea id="edit-memo" v-model="editForm.memo" rows="2" />
          </div>
          <button type="button" class="btn-primary" :disabled="isSavingEdit" @click="handleUpdate">
            {{ isSavingEdit ? '저장 중...' : '저장' }}
          </button>
          <button type="button" :disabled="isSavingEdit" @click="isEditing = false">취소</button>
        </template>
      </section>

      <!-- 곡 목록 -->
      <section>
        <div class="section-header">
          <h2 style="margin:0">곡 목록 ({{ items.length }}곡)</h2>
          <button type="button" @click="showAddItem = !showAddItem">
            {{ showAddItem ? '취소' : '+ 곡 추가' }}
          </button>
        </div>

        <!-- 곡 추가 폼 -->
        <div v-if="showAddItem" class="add-item-form">
          <p v-if="addError" class="error">{{ addError }}</p>
          <div class="form-field">
            <label for="add-song">곡 선택 *</label>
            <select id="add-song" v-model="addForm.songId">
              <option :value="null" disabled>곡을 선택하세요</option>
              <option v-for="song in songStore.songs" :key="song.songId" :value="song.songId">
                {{ song.title }}{{ song.artist ? ` — ${song.artist}` : '' }}
              </option>
            </select>
          </div>
          <div v-if="addForm.songId" class="form-field">
            <label for="add-sheet">악보 버전</label>
            <select id="add-sheet" v-model="addForm.songSheetId" :disabled="isLoadingSheets">
              <option :value="null">선택 안 함</option>
              <option
                v-for="sheet in availableSheets"
                :key="sheet.songSheetId"
                :value="sheet.songSheetId"
              >
                {{ sheetLabel(sheet.sheetKey, sheet.versionName) ?? '버전명 없음' }}
              </option>
            </select>
          </div>
          <div class="form-field">
            <label for="add-memo">메모</label>
            <input id="add-memo" v-model="addForm.memo" type="text" placeholder="선택사항" />
          </div>
          <button type="button" class="btn-primary" :disabled="isAddingItem" @click="handleAddItem">
            {{ isAddingItem ? '추가 중...' : '추가' }}
          </button>
        </div>

        <p v-if="items.length === 0" class="text-muted">곡이 없습니다.</p>
        <ol v-else class="item-list">
          <li v-for="item in items" :key="item.setlistItemId" class="item-row">
            <span class="item-no">{{ item.orderNo }}</span>
            <div class="item-info">
              <span class="item-song">{{ item.songTitle }}</span>
              <span v-if="item.songArtist" class="item-artist text-muted">{{ item.songArtist }}</span>
              <span v-if="sheetLabel(item.sheetKey, item.versionName)" class="item-sheet">
                {{ sheetLabel(item.sheetKey, item.versionName) }}
              </span>
              <span v-if="item.memo" class="item-memo text-muted">{{ item.memo }}</span>
            </div>
            <button
              type="button"
              class="btn-danger btn-sm"
              @click="handleDeleteItem(item.setlistItemId, item.songTitle)"
            >삭제</button>
          </li>
        </ol>
      </section>
    </template>
  </DefaultLayout>
</template>

<style scoped>
.detail-back { margin-bottom: 20px; }

.btn-back {
  background: none;
  border: none;
  padding: 0;
  color: #1971c2;
  font-size: 14px;
  cursor: pointer;
  margin-left: 0;
}
.btn-back:hover { text-decoration: underline; }

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
  margin-bottom: 8px;
}

.setlist-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.setlist-date {
  font-size: 14px;
  color: #495057;
  font-weight: 500;
}

.setlist-type {
  background: #e8f4fd;
  color: #1971c2;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-title {
  margin: 0;
  font-size: 22px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.setlist-memo {
  margin: 0;
  font-size: 13px;
  white-space: pre-line;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.add-item-form {
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

.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  counter-reset: none;
}

.item-row {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-no {
  font-size: 13px;
  font-weight: 700;
  color: #adb5bd;
  min-width: 20px;
  text-align: center;
}

.item-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.item-song {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a2e;
}

.item-artist {
  font-size: 13px;
}

.item-sheet {
  background: #f3f0ff;
  color: #7048e8;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.item-memo {
  font-size: 13px;
}

.btn-primary {
  background: #1971c2;
  color: #fff;
  border-color: #1971c2;
}
.btn-primary:hover:not(:disabled) { background: #1864ab; }

.btn-danger {
  background: #fff;
  color: #c92a2a;
  border-color: #ffc9c9;
}
.btn-danger:hover:not(:disabled) { background: #fff5f5; }

.btn-sm { padding: 4px 10px; font-size: 12px; }
</style>
