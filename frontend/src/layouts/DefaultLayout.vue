<script setup lang="ts">
import { Sun, Moon } from '@lucide/vue'
import { useTheme } from '../composables/useTheme'

const { theme, toggleTheme } = useTheme()
</script>

<template>
  <div class="min-h-screen bg-background text-foreground flex flex-col">
    <header class="sticky top-0 z-50 bg-background/80 backdrop-blur border-b border-border">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 h-14 flex items-center gap-4 sm:gap-8">
        <RouterLink to="/" class="text-base font-bold text-primary tracking-tight shrink-0">
          Worship Sheet
        </RouterLink>
        <nav class="flex gap-1 flex-1">
          <RouterLink
            to="/songs"
            active-class="bg-primary-soft text-primary font-semibold"
            class="px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            곡 목록
          </RouterLink>
          <RouterLink
            to="/setlists"
            active-class="bg-primary-soft text-primary font-semibold"
            class="px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            셋리스트
          </RouterLink>
        </nav>
        <button
          type="button"
          @click="toggleTheme"
          :aria-label="theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'"
          class="shrink-0 inline-flex items-center justify-center w-9 h-9 rounded-md text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
        >
          <Sun v-if="theme === 'dark'" class="w-4 h-4" />
          <Moon v-else class="w-4 h-4" />
        </button>
      </div>
    </header>

    <main class="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-6 sm:py-8">
      <slot />
    </main>
  </div>
</template>
