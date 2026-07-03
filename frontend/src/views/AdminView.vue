<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Shield, Users, Trash2, RotateCcw, Music, ListMusic } from '@lucide/vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Card from '../components/ui/Card.vue'
import Badge from '../components/ui/Badge.vue'
import { useAuthStore } from '../stores/authStore'
import {
  getAdminUsers, changeUserRole, deleteUser,
  getDeletedSongs, restoreSong, getDeletedSetlists, restoreSetlist,
} from '../apis/adminApi'
import type { AdminUser, DeletedSong, DeletedSetlist, UserRole } from '../types/admin'
import { ROLE_LABEL } from '../types/admin'
import { extractApiError } from '../composables/useApiError'

const authStore = useAuthStore()

type Tab = 'users' | 'trash'
const tab = ref<Tab>('users')

const users = ref<AdminUser[]>([])
const deletedSongs = ref<DeletedSong[]>([])
const deletedSetlists = ref<DeletedSetlist[]>([])
const isLoading = ref(false)
const errorMessage = ref('')

const loadUsers = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    users.value = await getAdminUsers()
  } catch (e) {
    errorMessage.value = extractApiError(e, '사용자 목록을 불러오지 못했습니다.')
  } finally {
    isLoading.value = false
  }
}

const loadTrash = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    ;[deletedSongs.value, deletedSetlists.value] = await Promise.all([
      getDeletedSongs(),
      getDeletedSetlists(),
    ])
  } catch (e) {
    errorMessage.value = extractApiError(e, '휴지통을 불러오지 못했습니다.')
  } finally {
    isLoading.value = false
  }
}

const switchTab = async (next: Tab) => {
  tab.value = next
  if (next === 'users') await loadUsers()
  else await loadTrash()
}

const isSelf = (user: AdminUser) => user.username === authStore.username

const handleToggleRole = async (user: AdminUser) => {
  const nextRole: UserRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  const label = nextRole === 'ADMIN' ? '관리자로 지정' : '일반 사용자로 변경'
  if (!confirm(`"${user.username}"을 ${label}할까요?`)) return
  try {
    await changeUserRole(user.userId, nextRole)
    await loadUsers()
  } catch (e) {
    alert(extractApiError(e, '권한 변경에 실패했습니다.'))
  }
}

const handleDeleteUser = async (user: AdminUser) => {
  if (!confirm(`"${user.username}" 계정을 삭제할까요? 되돌릴 수 없습니다.`)) return
  try {
    await deleteUser(user.userId)
    await loadUsers()
  } catch (e) {
    alert(extractApiError(e, '계정 삭제에 실패했습니다.'))
  }
}

const handleRestoreSong = async (song: DeletedSong) => {
  if (!confirm(`곡 "${song.title}"을 복구할까요?`)) return
  try {
    await restoreSong(song.songId)
    await loadTrash()
  } catch (e) {
    alert(extractApiError(e, '복구에 실패했습니다.'))
  }
}

const handleRestoreSetlist = async (setlist: DeletedSetlist) => {
  if (!confirm(`콘티 "${setlist.title ?? '(제목 없음)'}"을 복구할까요?`)) return
  try {
    await restoreSetlist(setlist.setlistId)
    await loadTrash()
  } catch (e) {
    alert(extractApiError(e, '복구에 실패했습니다.'))
  }
}

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

const trashEmpty = computed(() => deletedSongs.value.length === 0 && deletedSetlists.value.length === 0)

onMounted(loadUsers)
</script>

<template>
  <DefaultLayout>
    <div class="flex items-center gap-2 mb-5">
      <Shield class="w-5 h-5 text-primary" />
      <h1 class="text-xl font-bold text-foreground">관리자</h1>
    </div>

    <!-- 탭 -->
    <div class="flex gap-1 mb-6 border-b border-border">
      <button
        type="button"
        class="flex items-center gap-1.5 px-3 py-2 text-sm font-medium border-b-2 -mb-px transition-colors"
        :class="tab === 'users'
          ? 'border-primary text-primary'
          : 'border-transparent text-muted-foreground hover:text-foreground'"
        @click="switchTab('users')"
      >
        <Users class="w-4 h-4" />
        사용자
      </button>
      <button
        type="button"
        class="flex items-center gap-1.5 px-3 py-2 text-sm font-medium border-b-2 -mb-px transition-colors"
        :class="tab === 'trash'
          ? 'border-primary text-primary'
          : 'border-transparent text-muted-foreground hover:text-foreground'"
        @click="switchTab('trash')"
      >
        <Trash2 class="w-4 h-4" />
        휴지통
      </button>
    </div>

    <p v-if="isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="errorMessage" class="text-sm text-destructive py-4">{{ errorMessage }}</p>

    <!-- 사용자 관리 -->
    <div v-else-if="tab === 'users'" class="flex flex-col gap-2">
      <Card
        v-for="user in users"
        :key="user.userId"
        class="flex items-center gap-3 px-5 py-3.5"
      >
        <Badge :variant="user.role === 'ADMIN' ? 'violet' : 'secondary'" class="shrink-0">
          {{ ROLE_LABEL[user.role] }}
        </Badge>
        <span class="flex-1 text-sm font-medium text-foreground truncate">
          {{ user.username }}
          <span v-if="isSelf(user)" class="text-xs text-muted-foreground">(나)</span>
        </span>
        <span class="text-xs text-muted-foreground shrink-0 hidden sm:inline">
          {{ formatDate(user.createdAt) }}
        </span>
        <template v-if="!isSelf(user)">
          <button
            type="button"
            class="h-7 px-2.5 rounded text-xs font-medium border border-border text-muted-foreground hover:bg-muted transition-colors"
            @click="handleToggleRole(user)"
          >
            {{ user.role === 'ADMIN' ? '관리자 해제' : '관리자 지정' }}
          </button>
          <button
            type="button"
            class="p-1.5 text-muted-foreground hover:text-destructive transition-colors"
            aria-label="계정 삭제"
            @click="handleDeleteUser(user)"
          >
            <Trash2 class="w-3.5 h-3.5" />
          </button>
        </template>
      </Card>
    </div>

    <!-- 휴지통 (복구) -->
    <div v-else class="flex flex-col gap-6">
      <div v-if="trashEmpty" class="py-16 flex flex-col items-center text-center">
        <div class="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-4">
          <Trash2 class="w-6 h-6 text-muted-foreground" />
        </div>
        <p class="text-sm font-medium text-foreground">휴지통이 비어 있습니다</p>
        <p class="text-sm text-muted-foreground mt-1">삭제된 곡·콘티가 여기에 표시됩니다.</p>
      </div>

      <template v-else>
        <!-- 삭제된 곡 -->
        <section v-if="deletedSongs.length" class="flex flex-col gap-2">
          <div class="flex items-center gap-1.5 text-sm font-semibold text-muted-foreground">
            <Music class="w-4 h-4" /> 삭제된 곡 ({{ deletedSongs.length }})
          </div>
          <Card
            v-for="song in deletedSongs"
            :key="song.songId"
            class="flex items-center gap-3 px-5 py-3.5"
          >
            <span class="flex-1 text-sm font-medium text-foreground truncate">
              {{ song.title }}
              <span v-if="song.artist" class="text-xs text-muted-foreground">· {{ song.artist }}</span>
            </span>
            <span class="text-xs text-muted-foreground shrink-0 hidden sm:inline">
              {{ formatDate(song.deletedAt) }} 삭제
            </span>
            <button
              type="button"
              class="inline-flex items-center gap-1 h-7 px-2.5 rounded text-xs font-medium border border-border text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              @click="handleRestoreSong(song)"
            >
              <RotateCcw class="w-3.5 h-3.5" /> 복구
            </button>
          </Card>
        </section>

        <!-- 삭제된 콘티 -->
        <section v-if="deletedSetlists.length" class="flex flex-col gap-2">
          <div class="flex items-center gap-1.5 text-sm font-semibold text-muted-foreground">
            <ListMusic class="w-4 h-4" /> 삭제된 콘티 ({{ deletedSetlists.length }})
          </div>
          <Card
            v-for="setlist in deletedSetlists"
            :key="setlist.setlistId"
            class="flex items-center gap-3 px-5 py-3.5"
          >
            <span class="flex-1 text-sm font-medium text-foreground truncate">
              {{ setlist.title ?? '(제목 없음)' }}
              <span class="text-xs text-muted-foreground">· {{ setlist.serviceDate }}</span>
            </span>
            <span class="text-xs text-muted-foreground shrink-0 hidden sm:inline">
              {{ formatDate(setlist.deletedAt) }} 삭제
            </span>
            <button
              type="button"
              class="inline-flex items-center gap-1 h-7 px-2.5 rounded text-xs font-medium border border-border text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              @click="handleRestoreSetlist(setlist)"
            >
              <RotateCcw class="w-3.5 h-3.5" /> 복구
            </button>
          </Card>
        </section>
      </template>
    </div>
  </DefaultLayout>
</template>
