<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { Plus, Trash2, ChevronRight, X, Music } from '@lucide/vue'
import { createSetlist, deleteSetlist } from '../apis/setlistApi'
import { addSetlistItem } from '../apis/setlistItemApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'
import DatePicker from '../components/ui/DatePicker.vue'
import Card from '../components/ui/Card.vue'
import SongPickerModal from '../components/SongPickerModal.vue'
import { useSetlistStore } from '../stores/setlistStore'
import { useSongStore } from '../stores/songStore'
import type { Setlist } from '../types/setlist'

interface ApiErrorResponse { message?: string }

const router = useRouter()
const store = useSetlistStore()
const songStore = useSongStore()

const apiError = (e: unknown, fallback: string) =>
  isAxiosError<ApiErrorResponse>(e) ? (e.response?.data?.message ?? fallback) : fallback

const today = () => new Date().toISOString().slice(0, 10)

// 콘티 화면 진입 시 등록 폼을 바로 펼쳐 보여준다
const showCreateForm = ref(true)
const createForm = reactive({ serviceDate: today(), title: '', memo: '' })
const createError = ref('')
const isCreating = ref(false)

// 곡 추가 (폼 내)
interface PendingSong { songId: number; songSheetId: number | null; title: string }
const pendingSongs = ref<PendingSong[]>([])
const showSongPicker = ref(false)

const onSongPicked = (songId: number, songSheetId: number | null) => {
  const song = songStore.songs.find(s => s.songId === songId)
  if (!song) return
  pendingSongs.value.push({ songId, songSheetId, title: song.title })
  showSongPicker.value = false
}

// 곡 추가 모달을 열 때 비로소 곡 목록을 로드(진입 시 미리 받지 않음)
const openSongPicker = () => {
  void songStore.ensureSongsLoaded()
  showSongPicker.value = true
}

const removePendingSong = (idx: number) => { pendingSongs.value.splice(idx, 1) }

const resetCreateForm = () => {
  createForm.serviceDate = today()
  createForm.title = ''
  createForm.memo = ''
  createError.value = ''
  pendingSongs.value = []
}

const handleCreate = async () => {
  if (!createForm.serviceDate) {
    createError.value = '날짜는 필수입니다.'
    return
  }
  isCreating.value = true
  createError.value = ''
  try {
    const created = await createSetlist({
      serviceDate: createForm.serviceDate,
      title: createForm.title.trim() || null,
      memo: createForm.memo.trim() || null,
    })
    for (let i = 0; i < pendingSongs.value.length; i++) {
      const s = pendingSongs.value[i]
      await addSetlistItem(created.setlistId, { songId: s.songId, songSheetId: s.songSheetId ?? undefined, orderNo: i + 1, memo: null })
    }
    resetCreateForm()
    showCreateForm.value = false
    await router.push(`/setlists/${created.setlistId}`)
  } catch (e) {
    createError.value = apiError(e, '생성에 실패했습니다.')
  } finally {
    isCreating.value = false
  }
}

const handleDelete = async (setlist: Setlist, event: Event) => {
  event.stopPropagation()
  const label = setlist.title ?? setlist.serviceDate
  if (!confirm(`"${label}" 콘티를 삭제할까요?`)) return
  try {
    await deleteSetlist(setlist.setlistId)
    await store.fetchSetlists()
  } catch (e) {
    alert(apiError(e, '삭제에 실패했습니다.'))
  }
}

const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-')
  return `${y}.${m}.${d}`
}

onMounted(() => { void store.fetchSetlists() })
</script>

<template>
  <DefaultLayout>
    <SongPickerModal
      v-if="showSongPicker"
      :songs="songStore.songs"
      @select="onSongPicked"
      @close="showSongPicker = false"
    />

    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold text-foreground">콘티</h1>
      <Button @click="showCreateForm = !showCreateForm" :variant="showCreateForm ? 'outline' : 'default'">
        <template v-if="showCreateForm">
          <X class="w-4 h-4" />
          취소
        </template>
        <template v-else>
          <Plus class="w-4 h-4" />
          새 콘티
        </template>
      </Button>
    </div>

    <!-- 생성 폼 -->
    <Card v-if="showCreateForm" class="p-5 mb-6 bg-muted/40">
      <h2 class="text-sm font-semibold text-foreground mb-4">새 콘티 만들기</h2>
      <p v-if="createError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4">{{ createError }}</p>
      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1.5">
          <Label>날짜 <span class="text-destructive">*</span></Label>
          <DatePicker v-model="createForm.serviceDate" inline />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label for="setlist-title">제목</Label>
          <Input id="setlist-title" v-model="createForm.title" type="text" placeholder="선택사항" />
        </div>
        <!-- 곡 목록 -->
        <div class="flex flex-col gap-1.5">
          <div class="flex items-center justify-between">
            <Label>곡 목록</Label>
            <button
              type="button"
              class="inline-flex items-center gap-1 h-7 px-2.5 rounded-md border border-border text-xs font-medium text-foreground hover:bg-muted transition-colors"
              @click="openSongPicker"
            >
              <Plus class="w-3.5 h-3.5" />
              곡 추가
            </button>
          </div>
          <div v-if="pendingSongs.length === 0" class="text-xs text-muted-foreground py-2">
            추가된 곡이 없습니다.
          </div>
          <div v-else class="flex flex-col gap-1">
            <div
              v-for="(s, i) in pendingSongs"
              :key="i"
              class="flex items-center gap-2 px-3 py-1.5 rounded-md bg-muted/50"
            >
              <span class="text-xs text-muted-foreground w-5 shrink-0">{{ i + 1 }}</span>
              <Music class="w-3.5 h-3.5 text-muted-foreground shrink-0" />
              <span class="text-xs text-foreground flex-1 truncate">{{ s.title }}</span>
              <button type="button" class="p-0.5 text-muted-foreground hover:text-destructive" @click="removePendingSong(i)">
                <X class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        <div class="flex gap-2">
          <Button :disabled="isCreating" @click="handleCreate">
            {{ isCreating ? '생성 중...' : '생성' }}
          </Button>
        </div>
      </div>
    </Card>

    <p v-if="store.isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="store.errorMessage" class="text-sm text-destructive py-4">{{ store.errorMessage }}</p>
    <div v-else-if="store.setlists.length === 0" class="py-16 flex flex-col items-center text-center">
      <div class="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-4">
        <Plus class="w-6 h-6 text-muted-foreground" />
      </div>
      <p class="text-sm font-medium text-foreground">콘티가 없습니다</p>
      <p class="text-sm text-muted-foreground mt-1">새 콘티를 만들어 예배 순서를 구성해보세요.</p>
    </div>

    <div v-else class="flex flex-col gap-2">
      <div
        v-for="setlist in store.setlists"
        :key="setlist.setlistId"
        class="bg-card rounded-xl border border-border px-5 py-4 flex items-center justify-between gap-4 hover:border-primary/50 hover:shadow-md transition-all cursor-pointer group"
        @click="$router.push(`/setlists/${setlist.setlistId}`)"
      >
        <div class="flex items-center gap-3 min-w-0">
          <span class="text-sm font-semibold text-foreground flex-shrink-0">{{ formatDate(setlist.serviceDate) }}</span>
          <span v-if="setlist.title" class="text-sm text-muted-foreground truncate">{{ setlist.title }}</span>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-xs text-muted-foreground">{{ setlist.items.length }}곡</span>
          <Button
            variant="destructive"
            size="sm"
            @click="handleDelete(setlist, $event)"
          >
            <Trash2 class="w-3.5 h-3.5" />
          </Button>
          <ChevronRight class="w-4 h-4 text-muted-foreground group-hover:text-primary transition-colors" />
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>
