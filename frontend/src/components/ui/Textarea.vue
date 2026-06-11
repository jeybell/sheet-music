<script setup lang="ts">
defineOptions({ inheritAttrs: false })
const model = defineModel<string>()
</script>

<template>
  <textarea
    v-model="model"
    class="flex min-h-[80px] w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-50 resize-vertical"
    v-bind="$attrs"
  />
</template>
