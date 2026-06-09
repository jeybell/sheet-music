<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink to="/" class="app-logo">Worship Sheet</RouterLink>
      <nav class="app-nav">
        <RouterLink to="/songs" active-class="is-active">곡 목록</RouterLink>
        <RouterLink to="/setlists" active-class="is-active">셋리스트</RouterLink>
      </nav>
    </header>

    <main class="app-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e9ecef;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 32px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.app-logo {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  text-decoration: none;
  letter-spacing: -0.3px;
}

.app-nav {
  display: flex;
  gap: 4px;
}

.app-nav a {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #6c757d;
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
}

.app-nav a:hover {
  background: #f1f3f5;
  color: #1a1a2e;
}

.app-nav a.is-active {
  background: #e8f4fd;
  color: #1971c2;
}

.app-main {
  flex: 1;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 24px;
  box-sizing: border-box;
}
</style>
