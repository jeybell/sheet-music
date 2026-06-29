<script setup lang="ts">
import { ref, computed } from 'vue'
import { X } from '@lucide/vue'

const props = defineProps<{
  modelValue: string[]
  suggestions?: string[]
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const input = ref('')

const filteredSuggestions = computed(() => {
  const q = input.value.trim().toLowerCase()
  if (!q || q.length < 1) return []
  return (props.suggestions ?? [])
    .filter(s => s.toLowerCase().includes(q) && !props.modelValue.includes(s))
    .slice(0, 8)
})

const addTag = (tag: string) => {
  const t = tag.trim()
  if (!t || props.modelValue.includes(t)) return
  emit('update:modelValue', [...props.modelValue, t])
  input.value = ''
}

const removeTag = (tag: string) => {
  emit('update:modelValue', props.modelValue.filter(t => t !== tag))
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    addTag(input.value)
  } else if (e.key === 'Backspace' && input.value === '' && props.modelValue.length > 0) {
    removeTag(props.modelValue[props.modelValue.length - 1])
  }
}
</script>

<template>
  <div class="relative">
    <div
      class="flex flex-wrap gap-1.5 min-h-[2.5rem] px-3 py-2 border border-border rounded-md bg-background focus-within:ring-2 focus-within:ring-primary/50 cursor-text"
    >
      <span
        v-for="tag in modelValue"
        :key="tag"
        class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-primary/15 text-primary text-xs font-medium"
      >
        {{ tag }}
        <button type="button" class="hover:text-destructive transition-colors" @click="removeTag(tag)">
          <X class="w-3 h-3" />
        </button>
      </span>
      <input
        v-model="input"
        type="text"
        class="flex-1 min-w-[8rem] bg-transparent text-sm text-foreground placeholder:text-muted-foreground outline-none"
        :placeholder="modelValue.length === 0 ? (placeholder ?? '태그 입력 후 Enter') : ''"
        @keydown="onKeydown"
        @blur="() => { if (input.trim()) addTag(input) }"
      />
    </div>

    <!-- 자동완성 드롭다운 -->
    <div
      v-if="filteredSuggestions.length > 0"
      class="absolute top-full left-0 right-0 z-20 mt-1 bg-card border border-border rounded-lg shadow-lg overflow-hidden"
    >
      <button
        v-for="s in filteredSuggestions"
        :key="s"
        type="button"
        class="w-full text-left px-3 py-2 text-sm text-foreground hover:bg-muted transition-colors"
        @mousedown.prevent="addTag(s)"
      >
        {{ s }}
      </button>
    </div>
  </div>
</template>
