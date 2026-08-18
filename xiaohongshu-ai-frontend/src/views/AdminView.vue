<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AdminShell from '../components/AdminShell.vue'
import { getAdminDashboard } from '../services/adminApi'
import type { AdminDashboard } from '../types/admin'

defineEmits<{ navigate: [path: string] }>()
const dashboard = ref<AdminDashboard | null>(null)
const errorMessage = ref('')
const controller = new AbortController()
const cards = [
  { key: 'totalUsers', label: '用户总数', hint: '已注册账号' },
  { key: 'totalGenerations', label: '生成记录总数', hint: '全部任务' },
  { key: 'todayGenerations', label: '今日生成次数', hint: '今日创建任务' },
  { key: 'todayActiveUsers', label: '今日活跃用户', hint: '今日生成用户去重' },
] as const

onMounted(async () => {
  try { dashboard.value = await getAdminDashboard(controller.signal) }
  catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) errorMessage.value = error instanceof Error ? error.message : '统计数据加载失败。' }
})
onBeforeUnmount(() => controller.abort())
</script>

<template>
  <AdminShell active-path="/admin" title="数据概览" description="快速了解平台用户与内容生成情况。" @navigate="$emit('navigate', $event)">
    <div v-if="errorMessage" class="admin-panel admin-state admin-error">{{ errorMessage }}</div>
    <div v-else-if="!dashboard" class="admin-panel admin-state">正在加载统计数据…</div>
    <div v-else class="stats-grid">
      <article v-for="card in cards" :key="card.key" class="stat-card"><span>{{ card.label }}</span><strong>{{ dashboard[card.key] }}</strong><small>{{ card.hint }}</small></article>
    </div>
  </AdminShell>
</template>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.stat-card { padding: 28px; border: 1px solid var(--line); border-radius: 22px; background: rgba(255,255,255,.94); box-shadow: 0 20px 60px rgba(72,37,42,.06); }
.stat-card span { color: var(--muted); font-size: 14px; font-weight: 700; }
.stat-card strong { display: block; margin: 14px 0 10px; color: var(--ink); font-size: 42px; line-height: 1; }
.stat-card small { color: #aa8f92; }
@media (max-width: 560px) { .stats-grid { grid-template-columns: 1fr; } }
</style>
