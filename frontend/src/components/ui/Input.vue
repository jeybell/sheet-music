<script setup lang="ts">
defineOptions({ inheritAttrs: false })
const model = defineModel<string>()
</script>

<template>
  <input
    v-model="model"
    class="flex h-9 w-full rounded-md border border-zinc-200 bg-white px-3 py-1 text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-50"
    v-bind="$attrs"
  />
</template>
