<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink to="/songs" class="app-logo">Worship Sheet</RouterLink>
    </header>

    <main class="app-main">
      <slot />
    </main>
  </div>
</template>
