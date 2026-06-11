<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  variant?: 'default' | 'secondary' | 'violet' | 'blue' | 'destructive'
}>(), {
  variant: 'default',
})

const badgeClass = computed(() => cn(
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
  props.variant === 'default' && 'bg-zinc-100 text-zinc-700',
  props.variant === 'secondary' && 'bg-zinc-100 text-zinc-600',
  props.variant === 'violet' && 'bg-violet-100 text-violet-700',
  props.variant === 'blue' && 'bg-blue-100 text-blue-700',
  props.variant === 'destructive' && 'bg-red-100 text-red-700',
))
</script>

<template>
  <span :class="badgeClass">
    <slot />
  </span>
</template>
