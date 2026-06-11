<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { ChevronLeft, Pencil, Trash2, Plus, Upload, FileText, X } from '@lucide/vue'
import { deleteSong, updateSong } from '../apis/songApi'
import { deleteSongFile, uploadSongSheetFile } from '../apis/songFileApi'
import { createSongSheet, deleteSongSheet } from '../apis/songSheetApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import Badge from '../components/ui/Badge.vue'
import Card from '../components/ui/Card.vue'
import Separator from '../components/ui/Separator.vue'
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

// ── 곡 수정
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

// ── 곡 삭제
const handleDeleteSong = async () => {
  if (!confirm(`"${song.value?.title}" 곡을 삭제할까요?`)) return
  try {
    await deleteSong(props.songId)
    await router.push('/songs')
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

// ── 악보 버전 추가
const showAddSheet = ref(false)
const sheetForm = reactive({ sheetKey: '', versionName: '', memo: '' })
const sheetError = ref('')
const isCreatingSheet = ref(false)

const resetSheetForm = () => {
  sheetForm.sheetKey = ''
  sheetForm.versionName = ''
  sheetForm.memo = ''
  sheetError.value = ''
}

const handleCreateSheet = async () => {
  sheetError.value = ''
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

// ── 악보 버전 삭제
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

// ── 파일 업로드
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

// ── 파일 삭제
const handleDeleteFile = async (fileId: number, fileName: string) => {
  if (!confirm(`"${fileName}" 파일을 삭제할까요?`)) return
  try {
    await deleteSongFile(fileId)
    await songStore.fetchSong(props.songId)
  } catch (e) {
    alert(apiError(e, '파일 삭제에 실패했습니다.'))
  }
}

const loadSong = () => {
  if (Number.isFinite(props.songId)) void songStore.fetchSong(props.songId)
}

onMounted(loadSong)
watch(() => props.songId, loadSong)
</script>

<template>
  <DefaultLayout>
    <div class="mb-6">
      <button
        type="button"
        class="inline-flex items-center gap-1 text-sm text-zinc-400 hover:text-zinc-700 transition-colors"
        @click="$router.push('/songs')"
      >
        <ChevronLeft class="w-4 h-4" />
        목록으로
      </button>
    </div>

    <p v-if="songStore.isLoading" class="text-sm text-zinc-400 py-8 text-center">불러오는 중...</p>
    <p v-else-if="songStore.errorMessage" class="text-sm text-red-500">{{ songStore.errorMessage }}</p>

    <template v-else-if="song">
      <!-- 곡 정보 카드 -->
      <Card class="p-6 mb-6">
        <template v-if="!isEditing">
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <h1 class="text-2xl font-bold text-zinc-900 leading-tight">{{ song.title }}</h1>
              <div class="mt-3 flex flex-col gap-1.5 text-sm">
                <p v-if="song.artist" class="text-zinc-500">
                  <span class="text-zinc-400 mr-2">아티스트</span>{{ song.artist }}
                </p>
                <p v-if="song.composer" class="text-zinc-500">
                  <span class="text-zinc-400 mr-2">작곡가</span>{{ song.composer }}
                </p>
                <p v-if="song.memo" class="text-zinc-500 whitespace-pre-line mt-1">{{ song.memo }}</p>
              </div>
            </div>
            <div class="flex gap-2 flex-shrink-0">
              <Button variant="outline" size="sm" @click="startEdit">
                <Pencil class="w-3.5 h-3.5" />
                수정
              </Button>
              <Button variant="destructive" size="sm" @click="handleDeleteSong">
                <Trash2 class="w-3.5 h-3.5" />
                삭제
              </Button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-base font-semibold text-zinc-900">곡 수정</h2>
            <button type="button" class="text-zinc-400 hover:text-zinc-600" @click="cancelEdit">
              <X class="w-4 h-4" />
            </button>
          </div>
          <p v-if="editError" class="text-sm text-red-500 bg-red-50 rounded-md px-3 py-2 mb-4">{{ editError }}</p>
          <div class="flex flex-col gap-4">
            <div class="flex flex-col gap-1.5">
              <Label for="edit-title">제목 <span class="text-red-400">*</span></Label>
              <Input id="edit-title" v-model="editForm.title" type="text" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div class="flex flex-col gap-1.5">
                <Label for="edit-artist">아티스트</Label>
                <Input id="edit-artist" v-model="editForm.artist" type="text" />
              </div>
              <div class="flex flex-col gap-1.5">
                <Label for="edit-composer">작곡가</Label>
                <Input id="edit-composer" v-model="editForm.composer" type="text" />
              </div>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label for="edit-memo">메모</Label>
              <Textarea id="edit-memo" v-model="editForm.memo" rows="3" />
            </div>
            <div class="flex gap-2">
              <Button :disabled="isSavingEdit" @click="handleUpdateSong">
                {{ isSavingEdit ? '저장 중...' : '저장' }}
              </Button>
              <Button variant="outline" :disabled="isSavingEdit" @click="cancelEdit">취소</Button>
            </div>
          </div>
        </template>
      </Card>

      <!-- 악보 버전 섹션 -->
      <div>
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-base font-semibold text-zinc-900">악보 버전</h2>
          <Button variant="outline" size="sm" @click="showAddSheet = !showAddSheet">
            <template v-if="showAddSheet">
              <X class="w-3.5 h-3.5" />
              취소
            </template>
            <template v-else>
              <Plus class="w-3.5 h-3.5" />
              버전 추가
            </template>
          </Button>
        </div>

        <!-- 버전 추가 폼 -->
        <Card v-if="showAddSheet" class="p-5 mb-4 bg-zinc-50">
          <p v-if="sheetError" class="text-sm text-red-500 bg-red-50 rounded-md px-3 py-2 mb-4">{{ sheetError }}</p>
          <div class="grid grid-cols-2 gap-3 mb-3">
            <div class="flex flex-col gap-1.5">
              <Label for="sheet-key">키</Label>
              <Input id="sheet-key" v-model="sheetForm.sheetKey" type="text" placeholder="예) C, G, Am" />
            </div>
            <div class="flex flex-col gap-1.5">
              <Label for="version-name">버전명</Label>
              <Input id="version-name" v-model="sheetForm.versionName" type="text" placeholder="예) 원본, 남성용" />
            </div>
          </div>
          <div class="flex flex-col gap-1.5 mb-4">
            <Label for="sheet-memo">메모</Label>
            <Textarea id="sheet-memo" v-model="sheetForm.memo" rows="2" />
          </div>
          <Button :disabled="isCreatingSheet" @click="handleCreateSheet">
            {{ isCreatingSheet ? '추가 중...' : '추가' }}
          </Button>
        </Card>

        <!-- 버전 없음 -->
        <p v-if="sheets.length === 0" class="text-sm text-zinc-400 py-6 text-center">
          등록된 악보 버전이 없습니다.
        </p>

        <!-- 버전 목록 -->
        <div v-else class="flex flex-col gap-3">
          <Card v-for="sheet in sheets" :key="sheet.songSheetId" class="p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2 flex-wrap">
                <Badge v-if="sheet.sheetKey" variant="violet">{{ sheet.sheetKey }}</Badge>
                <span v-if="sheet.versionName" class="text-sm font-medium text-zinc-900">{{ sheet.versionName }}</span>
                <span v-if="!sheet.sheetKey && !sheet.versionName" class="text-sm text-zinc-400">버전명 없음</span>
              </div>
              <Button variant="destructive" size="sm" @click="handleDeleteSheet(sheet)">
                <Trash2 class="w-3.5 h-3.5" />
                삭제
              </Button>
            </div>

            <p v-if="sheet.memo" class="text-xs text-zinc-400 mb-3">{{ sheet.memo }}</p>

            <Separator class="mb-3" />

            <!-- 파일 목록 -->
            <div v-if="sheet.files?.length" class="flex flex-col gap-1.5 mb-3">
              <div
                v-for="file in sheet.files"
                :key="file.songFileId"
                class="flex items-center justify-between py-1.5 px-3 rounded-md bg-zinc-50"
              >
                <div class="flex items-center gap-2 min-w-0">
                  <FileText class="w-3.5 h-3.5 text-zinc-400 flex-shrink-0" />
                  <span class="text-xs text-zinc-600 truncate">
                    {{ file.originalFileName ?? file.storedFileName ?? '파일명 없음' }}
                  </span>
                </div>
                <button
                  type="button"
                  class="ml-2 text-zinc-400 hover:text-red-500 transition-colors flex-shrink-0"
                  @click="handleDeleteFile(file.songFileId, file.originalFileName ?? '파일')"
                >
                  <X class="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
            <p v-else class="text-xs text-zinc-400 mb-3">파일 없음</p>

            <!-- 파일 업로드 -->
            <div class="flex items-center gap-2 flex-wrap">
              <label
                class="flex items-center gap-1.5 h-8 px-3 rounded-md border border-zinc-200 bg-white text-xs text-zinc-600 hover:bg-zinc-50 cursor-pointer transition-colors"
              >
                <Upload class="w-3.5 h-3.5" />
                파일 선택
                <input
                  :key="`${sheet.songSheetId}-${uploadInputKeys[sheet.songSheetId] ?? 0}`"
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg"
                  class="hidden"
                  @change="handleFileChange($event, sheet.songSheetId)"
                />
              </label>
              <span v-if="selectedFiles[sheet.songSheetId]" class="text-xs text-zinc-500 truncate max-w-[160px]">
                {{ selectedFiles[sheet.songSheetId]?.name }}
              </span>
              <Button
                size="sm"
                :disabled="uploadingSheets[sheet.songSheetId] || !selectedFiles[sheet.songSheetId]"
                @click="handleUpload(sheet.songSheetId)"
              >
                {{ uploadingSheets[sheet.songSheetId] ? '업로드 중...' : '업로드' }}
              </Button>
              <span v-if="uploadMessages[sheet.songSheetId]" class="text-xs text-green-600">
                {{ uploadMessages[sheet.songSheetId] }}
              </span>
              <span v-if="uploadErrors[sheet.songSheetId]" class="text-xs text-red-500">
                {{ uploadErrors[sheet.songSheetId] }}
              </span>
            </div>
          </Card>
        </div>
      </div>
    </template>
  </DefaultLayout>
</template>
