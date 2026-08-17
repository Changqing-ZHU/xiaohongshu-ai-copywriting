<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getGenerationHistory, resolveApiUrl } from '../services/generationApi'
import type { GenerationHistoryItem, GenerationStatus } from '../types/generation'

const emit = defineEmits<{
  select: [record: GenerationHistoryItem]
}>()

const records = ref<GenerationHistoryItem[]>([])
const loading = ref(true)
const errorMessage = ref('')
const unavailableImages = ref(new Set<number>())
let controller: AbortController | null = null

const loadHistory = async () => {
  controller?.abort()
  controller = new AbortController()
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await getGenerationHistory(controller.signal)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    errorMessage.value = error instanceof Error ? error.message : '历史记录加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const statusText = (status: GenerationStatus) => {
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '生成失败'
  return '生成中'
}

const formatTime = (value: string) => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
}).format(new Date(value))

const markImageUnavailable = (id: number) => {
  unavailableImages.value = new Set([...unavailableImages.value, id])
}

onMounted(loadHistory)
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <div class="history-page page-container">
    <header class="history-heading">
      <div>
        <span>CREATION ARCHIVE</span>
        <h1>历史生成记录</h1>
        <p>回看每一次图片灵感，以及 AI 为你生成的标题、正文和话题标签。</p>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadHistory">
        {{ loading ? '加载中…' : '刷新记录' }}
      </button>
    </header>

    <section v-if="loading" class="state-card" aria-live="polite">
      <span class="spinner" aria-hidden="true"></span>
      <strong>正在读取历史记录</strong>
    </section>

    <section v-else-if="errorMessage" class="state-card failed">
      <strong>历史记录加载失败</strong>
      <p>{{ errorMessage }}</p>
      <button class="secondary-button" type="button" @click="loadHistory">重新加载</button>
    </section>

    <section v-else-if="records.length === 0" class="state-card">
      <strong>还没有生成记录</strong>
      <p>完成第一次图片文案生成后，记录会出现在这里。</p>
    </section>

    <section v-else class="history-grid" aria-label="历史生成列表">
      <button
        v-for="record in records"
        :key="record.id"
        class="history-card"
        type="button"
        @click="emit('select', record)"
      >
        <div class="thumbnail">
          <img
            v-if="record.imageUrl && !unavailableImages.has(record.id)"
            :src="resolveApiUrl(record.imageUrl)"
            :alt="record.title || `生成记录 ${record.id}`"
            loading="lazy"
            @error="markImageUnavailable(record.id)"
          />
          <span v-else>暂无图片</span>
        </div>
        <div class="record-body">
          <div class="record-meta">
            <span :class="['status', record.status.toLowerCase()]">{{ statusText(record.status) }}</span>
            <time :datetime="record.createdAt">{{ formatTime(record.createdAt) }}</time>
          </div>
          <h2>{{ record.title || (record.status === 'PROCESSING' ? '文案生成中…' : '未生成标题') }}</h2>
          <p>{{ record.content || (record.status === 'FAILED' ? '本次生成未完成。' : '正在等待生成结果。') }}</p>
          <div v-if="record.tags.length" class="tags">
            <span v-for="tag in record.tags" :key="tag">#{{ tag }}</span>
          </div>
          <small>查看详情 →</small>
        </div>
      </button>
    </section>
  </div>
</template>

<style scoped>
.history-page { padding: 58px 0 88px; }
.history-heading {
  margin-bottom: 34px; display: flex; align-items: flex-end;
  justify-content: space-between; gap: 24px;
}
.history-heading > div > span {
  color: var(--red); font-size: 12px; font-weight: 850; letter-spacing: 0.18em;
}
h1 { margin: 13px 0 9px; color: var(--ink); font-size: clamp(34px, 5vw, 50px); letter-spacing: -0.045em; }
.history-heading p { max-width: 620px; margin: 0; color: var(--muted); line-height: 1.7; }
.history-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; }
.history-card {
  padding: 0; overflow: hidden; display: grid; grid-template-columns: 190px minmax(0, 1fr);
  border: 1px solid var(--line); border-radius: 22px; text-align: left; cursor: pointer;
  color: inherit; background: var(--card); box-shadow: var(--shadow);
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
}
.history-card:hover { transform: translateY(-3px); border-color: #f0bdc3; box-shadow: 0 20px 45px rgba(97, 48, 56, 0.12); }
.history-card:focus-visible { outline: 3px solid rgba(255, 58, 80, 0.22); outline-offset: 3px; }
.thumbnail { min-height: 230px; display: grid; place-items: center; color: #aa9297; background: #f3eae8; }
.thumbnail img { width: 100%; height: 100%; object-fit: cover; }
.thumbnail span { font-size: 12px; }
.record-body { min-width: 0; padding: 22px; display: flex; flex-direction: column; }
.record-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.record-meta time { color: var(--muted); font-size: 11px; }
.status { padding: 5px 8px; border-radius: 999px; font-size: 11px; font-weight: 800; }
.status.completed { color: #177a5d; background: #e5f7ef; }
.status.processing { color: #966316; background: #fff0ca; }
.status.failed { color: #b4233a; background: #ffe6ea; }
h2 { margin: 18px 0 9px; overflow: hidden; color: var(--ink); font-size: 19px; text-overflow: ellipsis; white-space: nowrap; }
.record-body > p {
  margin: 0; overflow: hidden; display: -webkit-box; color: #6b5c60; font-size: 13px;
  line-height: 1.75; -webkit-box-orient: vertical; -webkit-line-clamp: 3;
}
.tags { margin-top: 14px; display: flex; gap: 7px; overflow: hidden; }
.tags span { color: var(--red); font-size: 11px; white-space: nowrap; }
.record-body small { margin-top: auto; padding-top: 18px; color: #9d737a; font-weight: 700; }
.state-card {
  min-height: 300px; padding: 40px; display: flex; align-items: center; justify-content: center;
  flex-direction: column; gap: 14px; border: 1px solid var(--line); border-radius: 24px;
  text-align: center; background: var(--card); box-shadow: var(--shadow);
}
.state-card strong { color: var(--ink); font-size: 20px; }
.state-card p { margin: 0; color: var(--muted); }
.state-card.failed { background: #fff9fa; }
.spinner {
  width: 36px; height: 36px; border: 4px solid #ffe0e4; border-top-color: var(--red);
  border-radius: 50%; animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 980px) { .history-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) {
  .history-heading { align-items: flex-start; flex-direction: column; }
  .history-card { grid-template-columns: 1fr; }
  .thumbnail { min-height: 240px; max-height: 340px; }
}
</style>
