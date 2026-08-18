<script setup lang="ts">
defineProps<{ activePath: string; title: string; description: string }>()
const emit = defineEmits<{ navigate: [path: string] }>()
const links = [
  { path: '/admin', label: '数据概览' },
  { path: '/admin/users', label: '用户管理' },
  { path: '/admin/generations', label: '生成记录' },
]
</script>

<template>
  <section class="admin-page">
    <div class="admin-layout">
      <aside class="admin-sidebar">
        <div class="sidebar-heading"><span>ADMIN</span><strong>管理中心</strong></div>
        <button v-for="link in links" :key="link.path" type="button" :class="{ active: activePath === link.path }" @click="emit('navigate', link.path)">{{ link.label }}</button>
      </aside>
      <div class="admin-content">
        <header class="page-heading"><span>ADMIN CONSOLE</span><h1>{{ title }}</h1><p>{{ description }}</p></header>
        <slot />
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-page { min-height: calc(100vh - 72px); padding: 46px 20px 80px; background: radial-gradient(circle at top, #fff0ef 0, var(--bg) 45%); }
.admin-layout { width: min(1180px, 100%); margin: 0 auto; display: grid; grid-template-columns: 210px minmax(0, 1fr); gap: 26px; }
.admin-sidebar { align-self: start; position: sticky; top: 98px; display: grid; gap: 8px; padding: 20px; border: 1px solid var(--line); border-radius: 24px; background: rgba(255,255,255,.92); box-shadow: 0 20px 60px rgba(72,37,42,.06); }
.sidebar-heading { display: grid; gap: 5px; padding: 6px 8px 18px; }
.sidebar-heading span, .page-heading > span { color: var(--red); font-size: 11px; font-weight: 800; letter-spacing: .16em; }
.sidebar-heading strong { color: var(--ink); font-size: 19px; }
.admin-sidebar button { padding: 12px 14px; border: 0; border-radius: 13px; color: var(--muted); background: transparent; cursor: pointer; text-align: left; font: inherit; font-size: 14px; font-weight: 700; }
.admin-sidebar button:hover, .admin-sidebar button.active { color: var(--red); background: #fff0f1; }
.admin-content { min-width: 0; }
.page-heading { margin-bottom: 28px; }
.page-heading h1 { margin: 10px 0 8px; color: var(--ink); font-size: clamp(32px, 5vw, 48px); }
.page-heading p { margin: 0; color: var(--muted); line-height: 1.7; }
:deep(.admin-panel) { overflow: hidden; border: 1px solid var(--line); border-radius: 24px; background: rgba(255,255,255,.94); box-shadow: 0 20px 60px rgba(72,37,42,.06); }
:deep(.admin-state) { padding: 42px; color: var(--muted); text-align: center; }
:deep(.admin-error) { color: #c92840; background: #fff2f3; }
@media (max-width: 760px) { .admin-layout { grid-template-columns: 1fr; } .admin-sidebar { position: static; grid-template-columns: repeat(3, 1fr); } .sidebar-heading { grid-column: 1 / -1; } .admin-sidebar button { text-align: center; } }
</style>
