<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, X, Trash2, ChevronDown } from '@lucide/vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Card from '../components/ui/Card.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'
import Badge from '../components/ui/Badge.vue'
import { getFeatureRequests, createFeatureRequest, updateFeatureRequestStatus, deleteFeatureRequest } from '../apis/featureRequestApi'
import type { FeatureRequest, FeatureRequestStatus } from '../types/featureRequest'
import { STATUS_LABEL, STATUS_VARIANT } from '../types/featureRequest'
import { extractApiError } from '../composables/useApiError'

const items = ref<FeatureRequest[]>([])
const isLoading = ref(false)
const errorMessage = ref('')

const showForm = ref(false)
const form = reactive({ title: '', content: '', author: '' })
const formError = ref('')
const isSubmitting = ref(false)

const expandedId = ref<number | null>(null)

const ALL_STATUSES: FeatureRequestStatus[] = ['PENDING', 'REVIEWING', 'DONE', 'HOLD']

const load = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    items.value = await getFeatureRequests()
  } catch (e) {
    errorMessage.value = extractApiError(e, '불러오지 못했습니다.')
  } finally {
    isLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    formError.value = '제목과 내용은 필수입니다.'
    return
  }
  isSubmitting.value = true
  formError.value = ''
  try {
    await createFeatureRequest({
      title: form.title.trim(),
      content: form.content.trim(),
      author: form.author.trim() || null,
    })
    form.title = ''
    form.content = ''
    form.author = ''
    showForm.value = false
    await load()
  } catch (e) {
    formError.value = extractApiError(e, '등록에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

const handleStatusChange = async (item: FeatureRequest, status: FeatureRequestStatus) => {
  try {
    await updateFeatureRequestStatus(item.featureRequestId, status)
    await load()
  } catch (e) {
    alert(extractApiError(e, '상태 변경에 실패했습니다.'))
  }
}

const handleDelete = async (item: FeatureRequest) => {
  if (!confirm(`"${item.title}"을 삭제할까요?`)) return
  try {
    await deleteFeatureRequest(item.featureRequestId)
    await load()
  } catch (e) {
    alert(extractApiError(e, '삭제에 실패했습니다.'))
  }
}

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

onMounted(load)
</script>

<template>
  <DefaultLayout>
    <div class="flex items-center justify-between mb-5">
      <h1 class="text-xl font-bold text-foreground">기능 요청</h1>
      <Button @click="showForm = !showForm" :variant="showForm ? 'outline' : 'default'">
        <template v-if="showForm">
          <X class="w-4 h-4" />
          취소
        </template>
        <template v-else>
          <Plus class="w-4 h-4" />
          요청하기
        </template>
      </Button>
    </div>

    <!-- 등록 폼 -->
    <Card v-if="showForm" class="p-5 mb-6 bg-muted/40">
      <p v-if="formError" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 mb-4">{{ formError }}</p>
      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1.5">
          <Label for="fr-title">제목 <span class="text-destructive">*</span></Label>
          <Input id="fr-title" v-model="form.title" type="text" placeholder="어떤 기능이 필요하신가요?" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label for="fr-content">내용 <span class="text-destructive">*</span></Label>
          <Textarea id="fr-content" v-model="form.content" rows="4" placeholder="기능에 대해 자세히 설명해주세요." />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label for="fr-author">작성자 (선택)</Label>
          <Input id="fr-author" v-model="form.author" type="text" placeholder="이름 또는 닉네임" />
        </div>
        <div class="flex gap-2">
          <Button :disabled="isSubmitting" @click="handleSubmit">
            {{ isSubmitting ? '등록 중...' : '등록' }}
          </Button>
        </div>
      </div>
    </Card>

    <p v-if="isLoading" class="text-sm text-muted-foreground py-8 text-center">불러오는 중...</p>
    <p v-else-if="errorMessage" class="text-sm text-destructive py-4">{{ errorMessage }}</p>

    <!-- 빈 상태 -->
    <div v-else-if="items.length === 0" class="py-16 flex flex-col items-center text-center">
      <div class="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-4">
        <Plus class="w-6 h-6 text-muted-foreground" />
      </div>
      <p class="text-sm font-medium text-foreground">아직 요청이 없습니다</p>
      <p class="text-sm text-muted-foreground mt-1">원하는 기능을 자유롭게 요청해주세요.</p>
    </div>

    <!-- 목록 -->
    <div v-else class="flex flex-col gap-2">
      <Card
        v-for="item in items"
        :key="item.featureRequestId"
        class="overflow-hidden"
      >
        <!-- 헤더 (클릭으로 펼치기) -->
        <button
          type="button"
          class="w-full flex items-center gap-3 px-5 py-4 hover:bg-muted/40 transition-colors text-left"
          @click="expandedId = expandedId === item.featureRequestId ? null : item.featureRequestId"
        >
          <Badge :variant="STATUS_VARIANT[item.status]" class="shrink-0">{{ STATUS_LABEL[item.status] }}</Badge>
          <span class="flex-1 text-sm font-medium text-foreground truncate">{{ item.title }}</span>
          <span class="text-xs text-muted-foreground shrink-0">
            {{ item.author ?? '익명' }} · {{ formatDate(item.createdAt) }}
          </span>
          <ChevronDown
            class="w-4 h-4 text-muted-foreground shrink-0 transition-transform duration-200"
            :class="{ 'rotate-180': expandedId === item.featureRequestId }"
          />
        </button>

        <!-- 펼쳐진 내용 -->
        <div v-if="expandedId === item.featureRequestId" class="px-5 pb-4 border-t border-border">
          <p class="text-sm text-foreground whitespace-pre-line leading-relaxed mt-4 mb-4">{{ item.content }}</p>

          <!-- 상태 변경 + 삭제 -->
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-xs text-muted-foreground">상태:</span>
            <button
              v-for="status in ALL_STATUSES"
              :key="status"
              type="button"
              class="h-6 px-2 rounded text-xs font-medium transition-colors"
              :class="item.status === status
                ? 'bg-primary text-primary-foreground'
                : 'border border-border text-muted-foreground hover:bg-muted'"
              @click="handleStatusChange(item, status)"
            >
              {{ STATUS_LABEL[status] }}
            </button>
            <div class="flex-1" />
            <button
              type="button"
              class="p-1.5 text-muted-foreground hover:text-destructive transition-colors"
              aria-label="삭제"
              @click="handleDelete(item)"
            >
              <Trash2 class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </Card>
    </div>
  </DefaultLayout>
</template>
