<script setup lang="ts">
defineProps<{ activePath: string }>()

const emit = defineEmits<{
  navigate: [path: string]
}>()
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <a class="brand" href="/" aria-label="红薯灵感首页" @click.prevent="emit('navigate', '/')">
        <span class="brand-mark">薯</span>
        <span>红薯灵感</span>
      </a>
      <nav aria-label="主要导航">
        <a href="/" :class="{ active: activePath === '/' }" @click.prevent="emit('navigate', '/')">
          首页
        </a>
        <a
          href="/workbench"
          :class="{ active: activePath === '/workbench' || activePath === '/result' }"
          @click.prevent="emit('navigate', '/workbench')"
        >
          文案生成
        </a>
        <a
          href="/history"
          :class="{ active: activePath === '/history' }"
          @click.prevent="emit('navigate', '/history')"
        >
          历史记录
        </a>
        <span class="stage-badge">AI 生成</span>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid rgba(238, 228, 226, 0.9);
  background: rgba(255, 249, 247, 0.86);
  backdrop-filter: blur(18px);
}
.navbar-inner {
  width: min(1120px, calc(100% - 40px));
  height: 72px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
  font-size: 19px;
  font-weight: 800;
  text-decoration: none;
  letter-spacing: -0.02em;
}
.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, var(--red), #ff7b66);
  box-shadow: 0 8px 20px rgba(255, 36, 66, 0.22);
}
nav { display: flex; align-items: center; gap: 20px; }
nav a {
  position: relative;
  color: var(--muted);
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}
nav a.active { color: var(--red); }
nav a.active::after {
  content: "";
  position: absolute;
  right: 2px;
  bottom: -10px;
  left: 2px;
  height: 3px;
  border-radius: 999px;
  background: var(--red);
}
.stage-badge {
  padding: 6px 10px;
  border-radius: 999px;
  color: #9a666f;
  font-size: 12px;
  background: #ffe8e9;
}
@media (max-width: 700px) {
  .navbar-inner { width: calc(100% - 24px); height: 64px; }
  .stage-badge { display: none; }
}
@media (max-width: 460px) {
  .brand > span:last-child { display: none; }
  nav { gap: 14px; }
  nav a { font-size: 13px; }
}
</style>
