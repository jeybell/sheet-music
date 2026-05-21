<script setup lang="ts">
import { reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { createSong } from '../apis/songApi'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import type { SongCreateRequest } from '../types/song'

interface ApiErrorResponse {
  message?: string
}

const router = useRouter()

const form = reactive<SongCreateRequest>({
  title: '',
  artist: '',
  composer: '',
  memo: '',
})

const isSaving = ref(false)
const errorMessage = ref('')

const toOptionalValue = (value: string) => {
  const trimmedValue = value.trim()
  return trimmedValue === '' ? null : trimmedValue
}

const getErrorMessage = (error: unknown) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? '곡을 저장하지 못했습니다.'
  }

  return '곡을 저장하지 못했습니다.'
}

const handleSubmit = async () => {
  errorMessage.value = ''

  const title = form.title.trim()
  if (!title) {
    errorMessage.value = 'title은 필수값입니다.'
    return
  }

  isSaving.value = true

  try {
    await createSong({
      title,
      artist: toOptionalValue(form.artist ?? ''),
      composer: toOptionalValue(form.composer ?? ''),
      memo: toOptionalValue(form.memo ?? ''),
    })
    await router.push('/songs')
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isSaving.value = false
  }
}

const handleCancel = () => {
  void router.push('/songs')
}
</script>

<template>
  <DefaultLayout>
    <h1>곡 등록</h1>

    <form @submit.prevent="handleSubmit">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <div class="form-field">
        <label for="title">title</label>
        <input id="title" v-model="form.title" type="text" required />
      </div>

      <div class="form-field">
        <label for="artist">artist</label>
        <input id="artist" v-model="form.artist" type="text" />
      </div>

      <div class="form-field">
        <label for="composer">composer</label>
        <input id="composer" v-model="form.composer" type="text" />
      </div>

      <div class="form-field">
        <label for="memo">memo</label>
        <textarea id="memo" v-model="form.memo" rows="4" />
      </div>

      <button type="submit" :disabled="isSaving">
        {{ isSaving ? '저장 중...' : '저장' }}
      </button>
      <button type="button" @click="handleCancel">취소</button>
    </form>
  </DefaultLayout>
</template>
