<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AdminShell from '../components/AdminShell.vue'
import { getAdminUsers } from '../services/adminApi'
import type { AdminUser } from '../types/admin'

defineEmits<{ navigate: [path: string] }>()
const users = ref<AdminUser[]>([])
const loading = ref(true)
const errorMessage = ref('')
const controller = new AbortController()
const formatTime = (value: string) => new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

onMounted(async () => {
  try { users.value = await getAdminUsers(controller.signal) }
  catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) errorMessage.value = error instanceof Error ? error.message : '用户列表加载失败。' }
  finally { loading.value = false }
})
onBeforeUnmount(() => controller.abort())
</script>

<template>
  <AdminShell active-path="/admin/users" title="用户管理" description="查看平台注册用户及其角色。" @navigate="$emit('navigate', $event)">
    <div class="admin-panel">
      <div v-if="loading" class="admin-state">正在加载用户列表…</div>
      <div v-else-if="errorMessage" class="admin-state admin-error">{{ errorMessage }}</div>
      <div v-else class="table-wrap">
        <table><thead><tr><th>用户名</th><th>角色</th><th>注册时间</th></tr></thead><tbody>
          <tr v-for="user in users" :key="user.username"><td class="primary">{{ user.username }}</td><td><span class="role" :class="user.role.toLowerCase()">{{ user.role }}</span></td><td>{{ formatTime(user.createdAt) }}</td></tr>
        </tbody></table>
        <div v-if="users.length === 0" class="admin-state">暂无用户</div>
      </div>
    </div>
  </AdminShell>
</template>

<style scoped>
.table-wrap { overflow-x: auto; } table { width: 100%; border-collapse: collapse; } th, td { padding: 17px 20px; border-bottom: 1px solid var(--line); color: var(--muted); font-size: 14px; text-align: left; } th { color: #927b7e; background: #fff9f8; font-size: 12px; letter-spacing: .04em; } tbody tr:last-child td { border-bottom: 0; } .primary { color: var(--ink); font-weight: 750; } .role { padding: 5px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; background: #f4f1f1; } .role.admin { color: var(--red); background: #fff0f1; }
</style>
