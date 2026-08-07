<script setup lang="ts">
import CopyCard from '../components/CopyCard.vue'
import type { GeneratedDraft } from '../types/generation'

defineProps<{ draft: GeneratedDraft | null }>()
const emit = defineEmits<{ restart: [] }>()

const analysis = ['画面主体清晰', '自然光线', '生活方式', '治愈氛围']
const generatedTitle = '被这一刻治愈了｜把生活过成喜欢的样子 ✨'
const generatedContent = `最近越来越喜欢记录生活里的小瞬间。

不需要刻意安排，也不需要多么盛大。一个舒服的角落、一束刚刚好的光，就足以让普通的一天变得闪闪发亮。

认真生活的人，总能在细节里找到属于自己的浪漫。愿我们都能慢一点，去感受当下，也收藏每一次不期而遇的美好。`
const generatedTags = ['生活碎片', '日常记录', '治愈系', '氛围感', '我的生活方式']
</script>

<template>
  <div class="result-page page-container">
    <template v-if="draft">
      <header class="result-heading">
        <div>
          <span class="success-pill">✓ 生成完成</span>
          <h1>你的灵感文案已准备好</h1>
          <p>这是静态原型中的模拟结果，你可以体验查看与复制流程。</p>
        </div>
        <button class="secondary-button" type="button" @click="emit('restart')">返回重新生成</button>
      </header>

      <div class="result-grid">
        <aside class="image-column">
          <div class="image-card">
            <img :src="draft.imageUrl" :alt="draft.fileName" />
            <div><strong>{{ draft.fileName }}</strong><span>本地预览 · 未上传服务器</span></div>
          </div>
          <section class="analysis-card">
            <div class="analysis-heading"><span>AI 图片分析</span><small>模拟结果</small></div>
            <p>画面呈现出自然、松弛的生活氛围，主体突出，适合创作生活方式类分享内容。</p>
            <div class="analysis-tags"><span v-for="item in analysis" :key="item">{{ item }}</span></div>
          </section>
        </aside>

        <section class="copy-column" aria-label="生成的文案结果">
          <CopyCard eyebrow="推荐标题" :title="generatedTitle" />
          <CopyCard eyebrow="小红书正文" :content="generatedContent" />
          <CopyCard eyebrow="话题标签" :tags="generatedTags" />
        </section>
      </div>
    </template>

    <section v-else class="empty-result">
      <span>还没有生成内容</span>
      <h1>先上传一张喜欢的图片吧</h1>
      <p>结果页刷新后不会保留本地图片，这是当前静态原型的正常状态。</p>
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
h1 { margin: 16px 0 8px; color: var(--ink); font-size: clamp(32px, 5vw, 48px); letter-spacing: -0.045em; }
.result-heading p, .empty-result p { margin: 0; color: var(--muted); line-height: 1.7; }
.result-grid { display: grid; grid-template-columns: minmax(280px, 0.72fr) minmax(0, 1.28fr); gap: 24px; align-items: start; }
.image-column, .copy-column { display: grid; gap: 18px; }
.image-card, .analysis-card, .copy-column { border: 1px solid var(--line); background: var(--card); box-shadow: var(--shadow); }
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
.analysis-tags { display: flex; flex-wrap: wrap; gap: 7px; }
.analysis-tags span { padding: 6px 9px; border-radius: 8px; color: #7a5b61; font-size: 11px; background: #f8efed; }
.copy-column { padding: 18px; border-radius: 24px; }
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
