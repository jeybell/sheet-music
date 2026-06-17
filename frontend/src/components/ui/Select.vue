<script setup lang="ts">
defineOptions({ inheritAttrs: false })
const model = defineModel<string | number | null>()
</script>

<template>
  <select
    v-model="model"
    class="flex h-9 w-full rounded-md border border-input bg-card px-3 py-1 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent disabled:cursor-not-allowed disabled:opacity-50"
    v-bind="$attrs"
  >
    <slot />
  </select>
</template>
