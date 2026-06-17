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
  props.variant === 'default' && 'bg-muted text-muted-foreground',
  props.variant === 'secondary' && 'bg-muted text-muted-foreground',
  props.variant === 'violet' && 'bg-primary-soft text-primary',
  props.variant === 'blue' && 'bg-primary-soft text-primary',
  props.variant === 'destructive' && 'bg-destructive-soft text-destructive',
))
</script>

<template>
  <span :class="badgeClass">
    <slot />
  </span>
</template>
