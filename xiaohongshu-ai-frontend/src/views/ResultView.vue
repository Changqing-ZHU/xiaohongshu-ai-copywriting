<script setup lang="ts">
import { computed } from 'vue'
import CopyCard from '../components/CopyCard.vue'
import type { GeneratedDraft } from '../types/generation'

const props = withDefaults(defineProps<{
  draft: GeneratedDraft | null
  returnLabel?: string
}>(), {
  returnLabel: '返回重新生成',
})
const emit = defineEmits<{ restart: [] }>()

const statusLabel = computed(() => {
  if (props.draft?.status === 'COMPLETED') return '✓ 生成完成'
  if (props.draft?.status === 'FAILED') {
    if (props.draft.failureType === 'IMAGE_SIZE') return '图片过大'
    if (props.draft.failureType === 'IMAGE_FORMAT') return '图片格式不支持'
    if (props.draft.failureType === 'URL_FORMAT') return 'URL格式错误'
    if (props.draft.failureType === 'URL_ACCESS') return 'URL访问失败'
    if (props.draft.failureType === 'NETWORK') return '网络错误'
    if (props.draft.failureType === 'AI') return 'AI 生成失败'
    return '生成失败'
  }
  return 'AI 正在生成'
})

const heading = computed(() => {
  if (props.draft?.status === 'COMPLETED') return '你的灵感文案已准备好'
  if (props.draft?.status === 'FAILED') return statusLabel.value
  return '正在读懂你的图片'
})

const description = computed(() => {
  if (props.draft?.status === 'COMPLETED') return 'AI 已完成图片分析，并生成标题、正文和话题标签。'
  if (props.draft?.status === 'FAILED') return props.draft.errorMessage || '生成失败，请稍后重试。'
  return '图片正在上传并交给 AI 分析，请稍候。'
})
</script>

<template>
  <div class="result-page page-container">
    <template v-if="draft">
      <header class="result-heading">
        <div>
          <span
            class="success-pill"
            :class="{ processing: draft.status === 'PROCESSING', failed: draft.status === 'FAILED' }"
          >{{ statusLabel }}</span>
          <h1>{{ heading }}</h1>
          <p>{{ description }}</p>
        </div>
        <button class="secondary-button" type="button" @click="emit('restart')">{{ returnLabel }}</button>
      </header>

      <div class="result-grid">
        <aside class="image-column">
          <div v-if="draft.imageUrl" class="image-card">
            <img :src="draft.imageUrl" :alt="draft.fileName" />
            <div>
              <strong>{{ draft.fileName }}</strong>
              <span>{{ draft.generationId ? `生成任务 #${draft.generationId}` : '正在创建生成任务' }}</span>
            </div>
          </div>
          <div v-else class="url-source-card">
            <span>图片 URL</span>
            <strong>{{ draft.sourceUrl }}</strong>
            <small>系统正在下载图片并交给 AI 进行视觉分析</small>
          </div>
          <section class="analysis-card">
            <div class="analysis-heading">
              <span>AI 图片分析</span>
              <small>{{ draft.status === 'COMPLETED' ? '真实结果' : statusLabel }}</small>
            </div>
            <p v-if="draft.status === 'COMPLETED'">{{ draft.imageAnalysis }}</p>
            <p v-else-if="draft.status === 'PROCESSING'">AI 正在识别画面主体、场景与氛围…</p>
            <p v-else>{{ draft.errorMessage || '图片分析未能完成。' }}</p>
          </section>
        </aside>

        <section class="copy-column" aria-label="生成的文案结果">
          <template v-if="draft.status === 'COMPLETED'">
            <CopyCard eyebrow="推荐标题" :title="draft.title || ''" />
            <CopyCard eyebrow="小红书正文" :content="draft.content || ''" />
            <CopyCard eyebrow="话题标签" :tags="draft.tags" />
          </template>
          <div v-else class="status-card" :class="{ failed: draft.status === 'FAILED' }">
            <span v-if="draft.status === 'PROCESSING'" class="spinner" aria-hidden="true"></span>
            <strong>{{ draft.status === 'PROCESSING' ? 'AI 文案生成中' : statusLabel }}</strong>
            <p>{{ description }}</p>
          </div>
        </section>
      </div>
    </template>

    <section v-else class="empty-result">
      <span>还没有生成内容</span>
      <h1>先上传一张喜欢的图片吧</h1>
      <p>选择图片后，即可创建任务并生成小红书文案。</p>
      <button class="primary-button" type="button" @click="emit('restart')">前往生成文案</button>
    </section>
  </div>
</template>

<style scoped>
.result-page { padding: 58px 0 88px; }
.result-heading { margin-bottom: 34px; display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
.success-pill {
  display: inline-flex; padding: 7px 11px; border-radius: 999px;
  color: #168866; font-size: 12px; font-weight: 800; background: #e8f8f1;
}
.success-pill.processing { color: #9a6515; background: #fff1cf; }
.success-pill.failed { color: #b4233a; background: #ffe7eb; }
h1 { margin: 16px 0 8px; color: var(--ink); font-size: clamp(32px, 5vw, 48px); letter-spacing: -0.045em; }
.result-heading p, .empty-result p { margin: 0; color: var(--muted); line-height: 1.7; }
.result-grid { display: grid; grid-template-columns: minmax(280px, 0.72fr) minmax(0, 1.28fr); gap: 24px; align-items: start; }
.image-column, .copy-column { display: grid; gap: 18px; }
.image-card, .analysis-card, .copy-column { border: 1px solid var(--line); background: var(--card); box-shadow: var(--shadow); }
.url-source-card {
  padding: 24px; display: grid; gap: 9px; overflow: hidden; border: 1px solid var(--line);
  border-radius: 22px; background: var(--card); box-shadow: var(--shadow);
}
.url-source-card span { color: var(--red); font-size: 12px; font-weight: 800; }
.url-source-card strong { overflow: hidden; color: var(--ink); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.url-source-card small { color: var(--muted); line-height: 1.6; }
.image-card, .analysis-card { overflow: hidden; border-radius: 22px; }
.image-card img { display: block; width: 100%; max-height: 430px; object-fit: contain; background: #f1e9e7; }
.image-card > div { padding: 15px 18px; display: flex; flex-direction: column; gap: 4px; }
.image-card strong { overflow: hidden; color: var(--ink); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.image-card span { color: var(--muted); font-size: 11px; }
.analysis-card { padding: 22px; }
.analysis-heading { display: flex; align-items: center; justify-content: space-between; }
.analysis-heading span { color: var(--ink); font-size: 15px; font-weight: 800; }
.analysis-heading small { color: #a07178; }
.analysis-card p { margin: 14px 0 18px; color: #64585c; font-size: 13px; line-height: 1.75; }
.copy-column { padding: 18px; border-radius: 24px; }
.status-card {
  min-height: 310px; padding: 42px; display: flex; align-items: center; justify-content: center;
  flex-direction: column; border: 1px solid var(--line); border-radius: 18px; text-align: center; background: #fff;
}
.status-card strong { margin-top: 18px; color: var(--ink); font-size: 20px; }
.status-card p { max-width: 430px; margin: 10px 0 0; color: var(--muted); line-height: 1.7; }
.status-card.failed { background: #fff9fa; }
.spinner {
  width: 38px; height: 38px; border: 4px solid #ffe0e4; border-top-color: var(--red);
  border-radius: 50%; animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-result {
  max-width: 620px; margin: 90px auto; padding: 54px; border: 1px solid var(--line);
  border-radius: 28px; text-align: center; background: #fff; box-shadow: var(--shadow);
}
.empty-result > span { color: var(--red); font-size: 13px; font-weight: 800; }
.empty-result .primary-button { margin-top: 28px; }
@media (max-width: 820px) {
  .result-heading { align-items: flex-start; flex-direction: column; }
  .result-grid { grid-template-columns: 1fr; }
}
@media (max-width: 520px) {
  .result-page { padding-top: 38px; }
  .empty-result { margin: 40px auto; padding: 34px 22px; }
}
</style>
