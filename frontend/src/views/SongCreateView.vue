<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import { createSong } from '../apis/songApi'
import { extractApiError } from '../composables/useApiError'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Textarea from '../components/ui/Textarea.vue'
import Label from '../components/ui/Label.vue'

const router = useRouter()

const form = reactive({
  title: '',
  artist: '',
  composer: '',
  memo: '',
})

const isSaving = ref(false)
const errorMessage = ref('')

const toOpt = (v: string) => v.trim() || null

const handleSubmit = async () => {
  errorMessage.value = ''
  const title = form.title.trim()
  if (!title) {
    errorMessage.value = '제목은 필수입니다.'
    return
  }
  isSaving.value = true
  try {
    await createSong({
      title,
      artist: toOpt(form.artist ?? ''),
      composer: toOpt(form.composer ?? ''),
      memo: toOpt(form.memo ?? ''),
    })
    await router.push('/songs')
  } catch (error) {
    errorMessage.value = extractApiError(error, '곡을 저장하지 못했습니다.')
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <DefaultLayout>
    <div class="mb-6">
      <button
        type="button"
        class="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
        @click="$router.push('/songs')"
      >
        <ChevronLeft class="w-4 h-4" />
        목록으로
      </button>
    </div>

    <div class="max-w-lg">
      <h1 class="text-xl font-bold text-foreground mb-6">곡 등록</h1>

      <div class="bg-card rounded-xl border border-border shadow-sm p-6">
        <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
          <p v-if="errorMessage" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 whitespace-pre-line">{{ errorMessage }}</p>

          <div class="flex flex-col gap-1.5">
            <Label for="title">제목 <span class="text-destructive">*</span></Label>
            <Input id="title" v-model="form.title" type="text" placeholder="곡 제목을 입력하세요" required />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="artist">아티스트</Label>
            <Input id="artist" v-model="form.artist" type="text" placeholder="아티스트명" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="composer">작곡가</Label>
            <Input id="composer" v-model="form.composer" type="text" placeholder="작곡가명" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="memo">메모</Label>
            <Textarea id="memo" v-model="form.memo" rows="3" placeholder="메모를 입력하세요" />
          </div>

          <div class="flex gap-2 pt-2">
            <Button type="submit" :disabled="isSaving">
              {{ isSaving ? '저장 중...' : '저장' }}
            </Button>
            <Button type="button" variant="outline" @click="$router.push('/songs')">취소</Button>
          </div>
        </form>
      </div>
    </div>
  </DefaultLayout>
</template>
