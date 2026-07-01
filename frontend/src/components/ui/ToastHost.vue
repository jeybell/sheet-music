<script setup lang="ts">
import { CheckCircle, AlertCircle } from '@lucide/vue'
import { useToast } from '../../composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="fixed bottom-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col items-center gap-2 pointer-events-none">
    <TransitionGroup name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="pointer-events-auto flex items-center gap-2 px-4 py-2.5 rounded-lg shadow-lg text-sm font-medium border max-w-[90vw]"
        :class="t.type === 'success'
          ? 'bg-card text-foreground border-border'
          : 'bg-destructive-soft text-destructive border-destructive/30'"
      >
        <CheckCircle v-if="t.type === 'success'" class="w-4 h-4 text-green-500 shrink-0" />
        <AlertCircle v-else class="w-4 h-4 shrink-0" />
        <span class="truncate">{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
