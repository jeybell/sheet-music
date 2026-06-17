<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { Plus, Trash2, ChevronRight, X } from '@lucide/vue'
import { createSetlist, deleteSetlist } from '../apis/setlistApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import Badge from '../components/ui/Badge.vue'
import Card from '../components/ui/Card.vue'
import { useSetlistStore } from '../stores/setlistStore'
import type { Setlist } from '../types/setlist'

interface ApiErrorResponse { message?: string }

const router = useRouter()
const store = useSetlistStore()

const apiError = (e: unknown, fallback: string) =>
  isAxiosError<ApiErrorResponse>(e) ? (e.response?.data?.message ?? fallback) : fallback

const showCreateForm = ref(false)
const createForm = reactive({ serviceDate: '', serviceType: '', title: '', memo: '' })
const createError = ref('')
const isCreating = ref(false)

const resetCreateForm = () => {
  createForm.serviceDate = ''
  createForm.serviceType = ''
  createForm.title = ''
  createForm.memo = ''
  createError.value = ''
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
      serviceType: createForm.serviceType.trim() || null,
      title: createForm.title.trim() || null,
      memo: createForm.memo.trim() || null,
    })
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
  if (!confirm(`"${label}" 셋리스트를 삭제할까요?`)) return
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

onMounted(() => store.fetchSetlists())
</script>

<template>
  <DefaultLayout>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold text-foreground">셋리스트</h1>
      <Button @click="showCreateForm = !showCreateForm" :variant="showCreateForm ? 'outline' : 'default'">
        <template v-if="showCreateForm">
          <X class="w-4 h-4" />
          취소
        </template>
        <template v-else>
          <Plus class="w-4 h-4" />
          새 셋리스트
        </template>
      </Button>
    </div>

    <!-- 생성 폼 -->
    <Card v-if="showCreateForm" class="p-5 mb-6 bg-muted/40">
      <h2 class="text-sm font-semibold text-foreground mb-4">새 셋리스트 만들기</h2>
      <p v-if="createError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4">{{ createError }}</p>
      <div class="flex flex-col gap-4">
        <div class="grid grid-cols-2 gap-3">
          <div class="flex flex-col gap-1.5">
            <Label for="service-date">날짜 <span class="text-destructive">*</span></Label>
            <Input id="service-date" v-model="createForm.serviceDate" type="date" />
          </div>
          <div class="flex flex-col gap-1.5">
            <Label for="service-type">예배 종류</Label>
            <Input id="service-type" v-model="createForm.serviceType" type="text" placeholder="예) 주일 1부" />
          </div>
        </div>
        <div class="flex flex-col gap-1.5">
          <Label for="setlist-title">제목</Label>
          <Input id="setlist-title" v-model="createForm.title" type="text" placeholder="선택사항" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label for="setlist-memo">메모</Label>
          <Textarea id="setlist-memo" v-model="createForm.memo" rows="2" />
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
      <p class="text-sm font-medium text-foreground">셋리스트가 없습니다</p>
      <p class="text-sm text-muted-foreground mt-1">새 셋리스트를 만들어 콘티를 구성해보세요.</p>
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
          <Badge v-if="setlist.serviceType" variant="blue">{{ setlist.serviceType }}</Badge>
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
