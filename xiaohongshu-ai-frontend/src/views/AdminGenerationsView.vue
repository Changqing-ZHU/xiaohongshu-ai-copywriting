<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AdminShell from '../components/AdminShell.vue'
import { getAdminGenerations } from '../services/adminApi'
import { resolveApiUrl } from '../services/generationApi'
import type { AdminGeneration } from '../types/admin'

defineEmits<{ navigate: [path: string] }>()
const records = ref<AdminGeneration[]>([])
const loading = ref(true)
const errorMessage = ref('')
const controller = new AbortController()
const formatTime = (value: string) => new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
const formatSize = (size: number | null) => size == null ? '—' : `${(size / 1024).toFixed(1)} KB`

onMounted(async () => {
  try { records.value = await getAdminGenerations(controller.signal) }
  catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) errorMessage.value = error instanceof Error ? error.message : '生成记录加载失败。' }
  finally { loading.value = false }
})
onBeforeUnmount(() => controller.abort())
</script>

<template>
  <AdminShell active-path="/admin/generations" title="生成记录" description="查看所有用户的内容生成任务。" @navigate="$emit('navigate', $event)">
    <div class="admin-panel">
      <div v-if="loading" class="admin-state">正在加载生成记录…</div>
      <div v-else-if="errorMessage" class="admin-state admin-error">{{ errorMessage }}</div>
      <div v-else class="table-wrap">
        <table><thead><tr><th>图片</th><th>用户</th><th>标题</th><th>状态</th><th>创建时间</th></tr></thead><tbody>
          <tr v-for="record in records" :key="record.id"><td><img v-if="record.imageUrl" :src="resolveApiUrl(record.imageUrl)" :alt="record.originalFileName || '生成图片'"><span v-else class="no-image">无图片</span><small>{{ record.imageContentType || '—' }} · {{ formatSize(record.imageSize) }}</small></td><td>{{ record.username || '旧数据/未绑定' }}</td><td class="title-cell">{{ record.title || '尚未生成标题' }}</td><td><span class="status" :class="record.status.toLowerCase()">{{ record.status }}</span></td><td>{{ formatTime(record.createdAt) }}</td></tr>
        </tbody></table>
        <div v-if="records.length === 0" class="admin-state">暂无生成记录</div>
      </div>
    </div>
  </AdminShell>
</template>

<style scoped>
.table-wrap { overflow-x: auto; } table { width: 100%; min-width: 820px; border-collapse: collapse; } th, td { padding: 15px 17px; border-bottom: 1px solid var(--line); color: var(--muted); font-size: 13px; text-align: left; vertical-align: middle; } th { color: #927b7e; background: #fff9f8; font-size: 12px; } tbody tr:last-child td { border-bottom: 0; } img, .no-image { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 12px; object-fit: cover; background: #f7f1f1; color: #ad999b; font-size: 11px; } td small { display: block; margin-top: 6px; color: #ad999b; white-space: nowrap; } .title-cell { max-width: 260px; color: var(--ink); font-weight: 700; } .status { padding: 5px 9px; border-radius: 999px; font-size: 10px; font-weight: 800; background: #f3f0f0; } .status.completed { color: #277a50; background: #eaf8f0; } .status.failed { color: #c12b42; background: #fff0f2; } .status.processing { color: #966d13; background: #fff7df; }
</style>
