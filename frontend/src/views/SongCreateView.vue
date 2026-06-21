<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, Upload, X, ScanText } from '@lucide/vue'
import { createSong } from '../apis/songApi'
import { createSongSheet } from '../apis/songSheetApi'
import { uploadSongSheetFile } from '../apis/songFileApi'
import { previewOcr } from '../apis/ocrApi'
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
  sheetKey: '',
  memo: '',
})

const imageFile = ref<File | null>(null)
const imagePreviewUrl = ref<string | null>(null)
const isOcrLoading = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')

const toOpt = (v: string) => v.trim() || null

const handleImageChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  imageFile.value = file
  imagePreviewUrl.value = URL.createObjectURL(file)

  isOcrLoading.value = true
  try {
    const result = await previewOcr(file)
    if (result.title && !form.title) form.title = result.title
    if (result.key && !form.sheetKey) form.sheetKey = result.key
  } catch {
    // OCR 실패해도 수동 입력으로 진행
  } finally {
    isOcrLoading.value = false
  }
}

const clearImage = () => {
  imageFile.value = null
  imagePreviewUrl.value = null
}

const handleSubmit = async () => {
  errorMessage.value = ''
  const title = form.title.trim()
  if (!title) {
    errorMessage.value = '제목은 필수입니다.'
    return
  }
  isSaving.value = true
  try {
    const song = await createSong({
      title,
      artist: toOpt(form.artist),
      memo: toOpt(form.memo),
    })

    if (imageFile.value) {
      const sheet = await createSongSheet(song.songId, {
        sheetKey: toOpt(form.sheetKey),
        versionName: null,
        memo: null,
      })
      await uploadSongSheetFile(sheet.songSheetId, imageFile.value)
      // 업로드 직후 상세 페이지로 이동, OCR은 백그라운드 처리 중
    }

    await router.push(`/songs/${song.songId}`)
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
        <form @submit.prevent="handleSubmit" class="flex flex-col gap-5">
          <p v-if="errorMessage" class="text-sm text-destructive bg-destructive-soft rounded-md px-3 py-2 whitespace-pre-line">{{ errorMessage }}</p>

          <!-- 악보 이미지 첨부 -->
          <div class="flex flex-col gap-1.5">
            <Label>악보 이미지 <span class="text-muted-foreground font-normal">(선택)</span></Label>

            <div v-if="!imageFile">
              <label
                class="flex flex-col items-center justify-center gap-2 h-32 rounded-lg border-2 border-dashed border-border bg-muted/30 hover:bg-muted/50 cursor-pointer transition-colors"
              >
                <Upload class="w-6 h-6 text-muted-foreground" />
                <span class="text-sm text-muted-foreground">이미지를 선택하세요</span>
                <span class="text-xs text-muted-foreground">PNG, JPG, JPEG</span>
                <input
                  type="file"
                  accept=".png,.jpg,.jpeg"
                  class="hidden"
                  @change="handleImageChange"
                />
              </label>
            </div>

            <div v-else class="relative">
              <img
                :src="imagePreviewUrl!"
                alt="악보 미리보기"
                class="w-full max-h-48 object-contain rounded-lg border border-border bg-muted/30"
              />
              <button
                type="button"
                class="absolute top-2 right-2 w-7 h-7 rounded-full bg-background/80 backdrop-blur border border-border flex items-center justify-center text-foreground hover:bg-background transition-colors"
                @click="clearImage"
              >
                <X class="w-3.5 h-3.5" />
              </button>
            </div>

            <p v-if="isOcrLoading" class="text-xs text-primary flex items-center gap-1 mt-1">
              <ScanText class="w-3.5 h-3.5 animate-pulse" /> OCR 분석 중... 제목과 코드를 자동으로 입력합니다.
            </p>
          </div>

          <div class="border-t border-border" />

          <!-- 곡 정보 -->
          <div class="flex flex-col gap-1.5">
            <Label for="title">제목 <span class="text-destructive">*</span></Label>
            <Input id="title" v-model="form.title" type="text" placeholder="곡 제목을 입력하세요" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="sheetKey">코드</Label>
            <Input id="sheetKey" v-model="form.sheetKey" type="text" placeholder="예) C, G, Am" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="artist">아티스트</Label>
            <Input id="artist" v-model="form.artist" type="text" placeholder="아티스트명" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="memo">메모</Label>
            <Textarea id="memo" v-model="form.memo" rows="3" placeholder="메모를 입력하세요" />
          </div>

          <div class="flex gap-2 pt-1">
            <Button type="submit" :disabled="isSaving || isOcrLoading">
              {{ isSaving ? '저장 중...' : isOcrLoading ? 'OCR 분석 중...' : '저장' }}
            </Button>
            <Button type="button" variant="outline" @click="$router.push('/songs')">취소</Button>
          </div>
        </form>
      </div>
    </div>
  </DefaultLayout>
</template>
