<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { isAxiosError } from "axios";
import { uploadSongSheetFile } from "../apis/songFileApi";
import { createSongSheet } from "../apis/songSheetApi";
import DefaultLayout from "../layouts/DefaultLayout.vue";
import { useSongStore } from "../stores/songStore";
import type {
  SongFile,
  SongSheetCreateRequest,
  SongSheetSummary,
} from "../types/song";

interface ApiErrorResponse {
  message?: string;
}

const props = defineProps<{
  songId: number;
}>();

const songStore = useSongStore();

const song = computed(() => songStore.selectedSong);
const sheets = computed(
  () => song.value?.sheets ?? song.value?.songSheets ?? [],
);
const songFiles = computed(() => song.value?.files ?? []);
const hasSongFilesField = computed(() => song.value?.files !== undefined);
const selectedFiles = ref<Record<number, File | undefined>>({});
const uploadInputKeys = ref<Record<number, number>>({});
const uploadMessages = ref<Record<number, string | undefined>>({});
const uploadErrors = ref<Record<number, string | undefined>>({});
const uploadingSheets = ref<Record<number, boolean | undefined>>({});
const sheetForm = reactive<SongSheetCreateRequest>({
  sheetKey: "",
  versionName: "",
  memo: "",
});
const isCreatingSheet = ref(false);
const createSheetErrorMessage = ref("");
const createSheetMessage = ref("");

const getDisplayText = (value: string | null | undefined, fallback: string) =>
  value?.trim() || fallback;

const getSheetId = (sheet: SongSheetSummary) => sheet.songSheetId;

const getFileName = (file: SongFile) =>
  getDisplayText(
    file.originalFileName ?? file.storedFileName ?? file.filePath,
    "파일명 없음",
  );

const getUploadErrorMessage = (error: unknown) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? "파일 업로드에 실패했습니다.";
  }

  return "파일 업로드에 실패했습니다.";
};

const toOptionalValue = (value: string | null | undefined) => {
  const trimmedValue = value?.trim() ?? "";
  return trimmedValue === "" ? null : trimmedValue;
};

const getCreateSheetErrorMessage = (error: unknown) => {
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? "악보 버전을 저장하지 못했습니다.";
  }

  return "악보 버전을 저장하지 못했습니다.";
};

const resetSheetForm = () => {
  sheetForm.sheetKey = "";
  sheetForm.versionName = "";
  sheetForm.memo = "";
  createSheetErrorMessage.value = "";
  createSheetMessage.value = "";
};

const handleFileChange = (event: Event, sheet: SongSheetSummary) => {
  const sheetId = getSheetId(sheet);
  if (sheetId === undefined) {
    return;
  }

  const input = event.target as HTMLInputElement;
  selectedFiles.value[sheetId] = input.files?.[0];
  uploadMessages.value[sheetId] = undefined;
  uploadErrors.value[sheetId] = undefined;
};

const handleUpload = async (sheet: SongSheetSummary) => {
  const sheetId = getSheetId(sheet);
  if (sheetId === undefined) {
    return;
  }

  uploadMessages.value[sheetId] = undefined;
  uploadErrors.value[sheetId] = undefined;

  const file = selectedFiles.value[sheetId];
  if (!file) {
    uploadMessages.value[sheetId] = "업로드할 파일을 선택해주세요.";
    return;
  }

  uploadingSheets.value[sheetId] = true;

  try {
    await uploadSongSheetFile(sheetId, file);
    selectedFiles.value[sheetId] = undefined;
    uploadInputKeys.value[sheetId] = (uploadInputKeys.value[sheetId] ?? 0) + 1;
    uploadMessages.value[sheetId] = "파일 업로드가 완료되었습니다.";
    await songStore.fetchSong(props.songId);
  } catch (error) {
    uploadErrors.value[sheetId] = getUploadErrorMessage(error);
  } finally {
    uploadingSheets.value[sheetId] = false;
  }
};

const handleCreateSheet = async () => {
  createSheetErrorMessage.value = "";
  createSheetMessage.value = "";
  isCreatingSheet.value = true;

  try {
    await createSongSheet(props.songId, {
      sheetKey: toOptionalValue(sheetForm.sheetKey),
      versionName: toOptionalValue(sheetForm.versionName),
      memo: toOptionalValue(sheetForm.memo),
    });
    resetSheetForm();
    createSheetMessage.value = "악보 버전이 등록되었습니다.";
    await songStore.fetchSong(props.songId);
  } catch (error) {
    createSheetErrorMessage.value = getCreateSheetErrorMessage(error);
  } finally {
    isCreatingSheet.value = false;
  }
};

const loadSong = () => {
  if (Number.isFinite(props.songId)) {
    void songStore.fetchSong(props.songId);
  }
};

onMounted(loadSong);
watch(() => props.songId, loadSong);
</script>

<template>
  <DefaultLayout>
    <button type="button" @click="$router.push('/songs')">
      목록으로 돌아가기
    </button>

    <section>
      <p v-if="songStore.isLoading">곡 정보를 불러오는 중입니다.</p>
      <p v-else-if="songStore.errorMessage" class="error">
        {{ songStore.errorMessage }}
      </p>
      <p v-else-if="!song">곡 정보를 찾을 수 없습니다.</p>
      <article v-else>
        <h1>{{ song.title }}</h1>
        <p>아티스트: {{ getDisplayText(song.artist, "아티스트 미상") }}</p>
        <p>작곡가: {{ getDisplayText(song.composer, "작곡가 미상") }}</p>
        <p>메모: {{ getDisplayText(song.memo, "메모 없음") }}</p>

        <section>
          <h2>악보</h2>

          <form @submit.prevent="handleCreateSheet">
            <h3>악보 버전 추가</h3>

            <p v-if="createSheetErrorMessage" class="error">
              {{ createSheetErrorMessage }}
            </p>
            <p v-if="createSheetMessage">{{ createSheetMessage }}</p>

            <div class="form-field">
              <label for="sheet-key">sheetKey</label>
              <input id="sheet-key" v-model="sheetForm.sheetKey" type="text" />
            </div>

            <div class="form-field">
              <label for="version-name">versionName</label>
              <input
                id="version-name"
                v-model="sheetForm.versionName"
                type="text"
              />
            </div>

            <div class="form-field">
              <label for="sheet-memo">memo</label>
              <textarea id="sheet-memo" v-model="sheetForm.memo" rows="3" />
            </div>

            <button type="submit" :disabled="isCreatingSheet">
              {{ isCreatingSheet ? "저장 중..." : "저장" }}
            </button>
            <button
              type="button"
              :disabled="isCreatingSheet"
              @click="resetSheetForm"
            >
              입력 초기화
            </button>
          </form>

          <p v-if="sheets.length === 0">등록된 악보가 없습니다.</p>
          <ul v-else>
            <li
              v-for="(sheet, index) in sheets"
              :key="getSheetId(sheet) ?? index"
            >
              <p>키: {{ getDisplayText(sheet.sheetKey, "키 미지정") }}</p>
              <p>
                버전: {{ getDisplayText(sheet.versionName, "버전명 없음") }}
              </p>
              <p v-if="sheet.memo">메모: {{ sheet.memo }}</p>

              <p v-if="!sheet.files?.length">등록된 파일 없음</p>
              <ul v-else>
                <li v-for="file in sheet.files" :key="file.songFileId">
                  {{ getFileName(file) }}
                </li>
              </ul>

              <div>
                <input
                  :key="`${getSheetId(sheet) ?? index}-${uploadInputKeys[getSheetId(sheet) ?? 0] ?? 0}`"
                  type="file"
                  @change="handleFileChange($event, sheet)"
                />
                <button
                  type="button"
                  :disabled="
                    getSheetId(sheet) === undefined ||
                    Boolean(uploadingSheets[getSheetId(sheet) ?? 0])
                  "
                  @click="handleUpload(sheet)"
                >
                  {{
                    uploadingSheets[getSheetId(sheet) ?? 0]
                      ? "업로드 중..."
                      : "업로드"
                  }}
                </button>
                <p v-if="uploadMessages[getSheetId(sheet) ?? 0]">
                  {{ uploadMessages[getSheetId(sheet) ?? 0] }}
                </p>
                <p v-if="uploadErrors[getSheetId(sheet) ?? 0]" class="error">
                  {{ uploadErrors[getSheetId(sheet) ?? 0] }}
                </p>
              </div>
            </li>
          </ul>
        </section>

        <section v-if="hasSongFilesField">
          <h2>파일</h2>
          <p v-if="songFiles.length === 0">등록된 파일 없음</p>
          <ul v-else>
            <li v-for="file in songFiles" :key="file.songFileId">
              {{ getFileName(file) }}
            </li>
          </ul>
        </section>
      </article>
    </section>
  </DefaultLayout>
</template>
