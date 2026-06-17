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
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
  'disabled:pointer-events-none disabled:opacity-50 cursor-pointer',
  props.variant === 'default' && 'bg-primary text-primary-foreground hover:opacity-90',
  props.variant === 'secondary' && 'bg-muted text-foreground hover:bg-muted/70',
  props.variant === 'destructive' && 'border border-destructive-border bg-destructive-soft text-destructive hover:bg-destructive/10',
  props.variant === 'ghost' && 'text-muted-foreground hover:bg-muted hover:text-foreground',
  props.variant === 'outline' && 'border border-border bg-card text-foreground hover:bg-muted',
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
