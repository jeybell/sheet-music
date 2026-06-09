<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { createSetlist, deleteSetlist } from '../apis/setlistApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
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
    <div class="list-header">
      <h1 style="margin:0">셋리스트</h1>
      <button type="button" @click="showCreateForm = !showCreateForm">
        {{ showCreateForm ? '취소' : '+ 새 셋리스트' }}
      </button>
    </div>

    <!-- 생성 폼 -->
    <div v-if="showCreateForm" class="create-form">
      <p v-if="createError" class="error">{{ createError }}</p>
      <div class="form-row">
        <div class="form-field">
          <label for="service-date">날짜 *</label>
          <input id="service-date" v-model="createForm.serviceDate" type="date" />
        </div>
        <div class="form-field">
          <label for="service-type">예배 종류</label>
          <input id="service-type" v-model="createForm.serviceType" type="text" placeholder="예) 주일 1부, 수요예배" />
        </div>
      </div>
      <div class="form-field">
        <label for="setlist-title">제목</label>
        <input id="setlist-title" v-model="createForm.title" type="text" placeholder="선택사항" />
      </div>
      <div class="form-field">
        <label for="setlist-memo">메모</label>
        <textarea id="setlist-memo" v-model="createForm.memo" rows="2" />
      </div>
      <button type="button" class="btn-primary" :disabled="isCreating" @click="handleCreate">
        {{ isCreating ? '생성 중...' : '생성' }}
      </button>
    </div>

    <p v-if="store.isLoading" class="text-muted">불러오는 중...</p>
    <p v-else-if="store.errorMessage" class="error">{{ store.errorMessage }}</p>
    <p v-else-if="store.setlists.length === 0" class="text-muted">셋리스트가 없습니다.</p>

    <ul v-else class="setlist-list">
      <li
        v-for="setlist in store.setlists"
        :key="setlist.setlistId"
        class="setlist-item"
        @click="$router.push(`/setlists/${setlist.setlistId}`)"
      >
        <div class="setlist-info">
          <span class="setlist-date">{{ formatDate(setlist.serviceDate) }}</span>
          <span v-if="setlist.serviceType" class="setlist-type">{{ setlist.serviceType }}</span>
          <span v-if="setlist.title" class="setlist-title">{{ setlist.title }}</span>
        </div>
        <div class="setlist-right">
          <span class="setlist-count text-muted">{{ setlist.items.length }}곡</span>
          <button type="button" class="btn-danger btn-sm" @click="handleDelete(setlist, $event)">삭제</button>
        </div>
      </li>
    </ul>
  </DefaultLayout>
</template>

<style scoped>
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.create-form {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.setlist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setlist-item {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.setlist-item:hover {
  border-color: #adb5bd;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.setlist-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.setlist-date {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a2e;
}

.setlist-type {
  background: #e8f4fd;
  color: #1971c2;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.setlist-title {
  font-size: 14px;
  color: #495057;
}

.setlist-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.setlist-count {
  font-size: 13px;
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
