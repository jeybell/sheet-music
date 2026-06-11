<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  variant?: 'default' | 'secondary' | 'destructive' | 'ghost' | 'outline'
  size?: 'default' | 'sm' | 'lg' | 'icon'
}>(), {
  variant: 'default',
  size: 'default',
})

const buttonClass = computed(() => cn(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-colors',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-500',
  'disabled:pointer-events-none disabled:opacity-50 cursor-pointer',
  props.variant === 'default' && 'bg-violet-600 text-white hover:bg-violet-700',
  props.variant === 'secondary' && 'bg-zinc-100 text-zinc-900 hover:bg-zinc-200',
  props.variant === 'destructive' && 'border border-red-200 bg-white text-red-500 hover:bg-red-50',
  props.variant === 'ghost' && 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900',
  props.variant === 'outline' && 'border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50',
  props.size === 'default' && 'h-9 px-4 py-2',
  props.size === 'sm' && 'h-8 px-3 text-xs',
  props.size === 'lg' && 'h-10 px-8',
  props.size === 'icon' && 'h-9 w-9 p-0',
))
</script>

<template>
  <button :class="buttonClass">
    <slot />
  </button>
</template>
