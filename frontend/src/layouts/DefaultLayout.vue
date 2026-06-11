<template>
  <div class="min-h-screen bg-zinc-50 flex flex-col">
    <header class="sticky top-0 z-50 bg-white border-b border-zinc-200">
      <div class="max-w-4xl mx-auto px-6 h-14 flex items-center gap-8">
        <RouterLink to="/" class="text-base font-bold text-violet-600 tracking-tight">
          Worship Sheet
        </RouterLink>
        <nav class="flex gap-1">
          <RouterLink
            to="/songs"
            active-class="bg-violet-50 text-violet-700 font-semibold"
            class="px-3 py-1.5 rounded-md text-sm font-medium text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900 transition-colors"
          >
            곡 목록
          </RouterLink>
          <RouterLink
            to="/setlists"
            active-class="bg-violet-50 text-violet-700 font-semibold"
            class="px-3 py-1.5 rounded-md text-sm font-medium text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900 transition-colors"
          >
            셋리스트
          </RouterLink>
        </nav>
      </div>
    </header>

    <main class="flex-1 max-w-4xl w-full mx-auto px-6 py-8">
      <slot />
    </main>
  </div>
</template>
