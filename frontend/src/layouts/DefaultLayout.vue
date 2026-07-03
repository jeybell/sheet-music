<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Sun, Moon, MessageSquarePlus, LogOut, Shield } from '@lucide/vue'
import ToastHost from '../components/ui/ToastHost.vue'
import { useTheme } from '../composables/useTheme'
import { isLoading } from '../composables/useHttpLoading'
import { useAuthStore } from '../stores/authStore'

const { theme, toggleTheme } = useTheme()
const loading = isLoading()
const busy = computed(() => loading.value > 0)

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground flex flex-col">
    <header class="sticky top-0 z-50 bg-background/80 backdrop-blur border-b border-border">
      <!-- 상단 로딩 바 -->
      <div class="absolute bottom-0 left-0 right-0 h-0.5 overflow-hidden">
        <div
          class="h-full bg-primary origin-left transition-all duration-300"
          :class="busy ? 'opacity-100 animate-loading-bar' : 'opacity-0 w-full'"
        />
      </div>

      <div class="max-w-5xl mx-auto px-3 sm:px-6 h-12 sm:h-14 flex items-center gap-2 sm:gap-8">
        <RouterLink to="/" class="text-base font-bold text-primary tracking-tight shrink-0">
          Worship Sheet
        </RouterLink>
        <nav class="flex gap-0.5 sm:gap-1 flex-1">
          <RouterLink
            to="/"
            exact-active-class="bg-primary-soft text-primary font-semibold"
            class="px-2.5 sm:px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            홈
          </RouterLink>
          <RouterLink
            to="/setlists"
            active-class="bg-primary-soft text-primary font-semibold"
            class="px-2.5 sm:px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            콘티
          </RouterLink>
          <RouterLink
            to="/songs"
            active-class="bg-primary-soft text-primary font-semibold"
            class="px-2.5 sm:px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            악보
          </RouterLink>
        </nav>
        <div class="flex items-center gap-0.5 shrink-0">
          <RouterLink
            v-if="isAdmin"
            to="/admin"
            active-class="bg-primary-soft text-primary"
            aria-label="관리자"
            title="관리자"
            class="inline-flex items-center justify-center w-9 h-9 rounded-md text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <Shield class="w-4 h-4" />
          </RouterLink>
          <RouterLink
            to="/feature-requests"
            active-class="bg-primary-soft text-primary"
            aria-label="기능 요청"
            title="기능 요청"
            class="inline-flex items-center justify-center w-9 h-9 rounded-md text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <MessageSquarePlus class="w-4 h-4" />
          </RouterLink>
          <button
            type="button"
            @click="toggleTheme"
            :aria-label="theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'"
            class="inline-flex items-center justify-center w-9 h-9 rounded-md text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <Sun v-if="theme === 'dark'" class="w-4 h-4" />
            <Moon v-else class="w-4 h-4" />
          </button>
          <button
            type="button"
            @click="handleLogout"
            aria-label="로그아웃"
            title="로그아웃"
            class="inline-flex items-center justify-center w-9 h-9 rounded-md text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <LogOut class="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>

    <main
      class="flex-1 max-w-5xl w-full mx-auto px-3 sm:px-6 py-4 sm:py-8 transition-opacity duration-200"
      :class="busy ? 'opacity-60 pointer-events-none' : 'opacity-100'"
    >
      <slot />
    </main>

    <ToastHost />
  </div>
</template>
