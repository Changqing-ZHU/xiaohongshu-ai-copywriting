<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import ImageUploader from '../components/ImageUploader.vue'
import UrlInput from '../components/UrlInput.vue'
import type { GenerationInput } from '../types/generation'

const emit = defineEmits<{
  generate: [input: GenerationInput]
}>()

const selectedFile = ref<File | null>(null)
const sourceUrl = ref('')
const previewUrl = ref('')
const isGenerating = ref(false)
const preparationError = ref('')
const maxImageSize = 10 * 1024 * 1024

const selectImage = (file: File) => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  if (file.size > maxImageSize) {
    previewUrl.value = ''
    selectedFile.value = null
    preparationError.value = '图片大小超过限制，请上传小于 10MB 的图片。'
    return
  }

  preparationError.value = ''
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

const readAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new Error('图片预览读取失败'))
    reader.readAsDataURL(file)
  })

const generate = async () => {
  if ((!selectedFile.value && !sourceUrl.value.trim()) || isGenerating.value) return

  if (sourceUrl.value.trim() && !isValidHttpUrl(sourceUrl.value)) {
    preparationError.value = 'URL格式错误，请输入有效的 HTTP 或 HTTPS 地址。'
    return
  }

  isGenerating.value = true
  const file = selectedFile.value

  try {
    emit('generate', {
      file,
      imageUrl: file ? await readAsDataUrl(file) : '',
      fileName: file?.name ?? '',
      fileSize: file?.size ?? 0,
      url: sourceUrl.value.trim(),
    })
  } catch (error) {
    preparationError.value = error instanceof Error ? error.message : '图片读取失败，请重新选择'
  } finally {
    isGenerating.value = false
  }
}

const isValidHttpUrl = (value: string) => {
  try {
    const url = new URL(value.trim())
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <div class="home-page page-container">
    <section class="hero-copy">
      <span class="eyebrow">AI CONTENT STUDIO</span>
      <h1>一张图片，写出<br /><em>让人心动</em>的小红书文案</h1>
      <p>上传图片或粘贴图片链接，即刻获得标题、正文与话题标签。</p>
      <div class="feature-pills" aria-label="产品特点">
        <span>✦ 智能识图</span><span>✦ 自然表达</span><span>✦ 一键复制</span>
      </div>
    </section>

    <section class="workspace">
      <div class="input-column">
        <ImageUploader :file="selectedFile" :preview-url="previewUrl" @select="selectImage" />
        <div class="input-divider"><span>或</span></div>
        <UrlInput v-model="sourceUrl" />
      </div>
      <aside class="action-panel">
        <div class="step-label">02</div>
        <h2>生成你的专属文案</h2>
        <p>我们将分析图片内容，并生成贴近小红书社区表达习惯的完整文案。</p>
        <ul>
          <li><span>1</span>识别画面主题与氛围</li>
          <li><span>2</span>提炼内容亮点</li>
          <li><span>3</span>生成标题、正文和标签</li>
        </ul>
        <button
          class="primary-button generate-button"
          type="button"
          :disabled="(!selectedFile && !sourceUrl.trim()) || isGenerating"
          @click="generate"
        >
          {{ isGenerating ? '正在生成…' : '生成小红书文案' }}
        </button>
        <small :class="{ error: preparationError }">
          {{ preparationError || (selectedFile || sourceUrl.trim() ? '素材已就绪，可以开始生成' : '请上传图片或输入图片 URL') }}
        </small>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.home-page { padding: 78px 0 88px; }
.hero-copy { max-width: 760px; margin: 0 auto 48px; text-align: center; }
.eyebrow { color: var(--red); font-size: 12px; font-weight: 850; letter-spacing: 0.2em; }
h1 {
  margin: 18px 0; color: var(--ink); font-size: clamp(42px, 6vw, 68px);
  line-height: 1.12; letter-spacing: -0.055em;
}
h1 em { color: var(--red); font-style: normal; }
.hero-copy > p { margin: 0; color: var(--muted); font-size: 17px; }
.feature-pills { margin-top: 24px; display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; }
.feature-pills span {
  padding: 7px 12px; border: 1px solid #f1dfdc; border-radius: 999px;
  color: #7a5b61; font-size: 12px; background: rgba(255, 255, 255, 0.72);
}
.workspace {
  display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.75fr);
  gap: 24px; align-items: stretch;
}
.input-column { display: grid; align-content: start; }
.input-divider { position: relative; height: 34px; display: grid; place-items: center; }
.input-divider::before { position: absolute; width: 100%; height: 1px; content: ''; background: var(--line); }
.input-divider span {
  position: relative; z-index: 1; padding: 3px 10px; border-radius: 999px;
  color: var(--muted); font-size: 11px; background: #fff8f6;
}
.action-panel {
  padding: 30px; display: flex; flex-direction: column; border-radius: 24px;
  color: #fff; background: linear-gradient(155deg, #34272b, #1f171a); box-shadow: var(--shadow);
}
.step-label {
  display: grid; width: 36px; height: 36px; place-items: center; border-radius: 11px;
  color: #ffbbc4; font-size: 12px; font-weight: 800; background: rgba(255, 255, 255, 0.09);
}
.action-panel h2 { margin: 24px 0 10px; font-size: 25px; line-height: 1.3; }
.action-panel > p { margin: 0; color: #cbbfc2; font-size: 14px; line-height: 1.75; }
ul { margin: 28px 0 34px; padding: 0; display: grid; gap: 16px; list-style: none; }
li { display: flex; align-items: center; gap: 11px; color: #f2eaec; font-size: 13px; }
li span {
  display: grid; width: 25px; height: 25px; place-items: center; border-radius: 50%;
  color: #ff97a5; font-size: 11px; font-weight: 800; background: rgba(255, 255, 255, 0.08);
}
.generate-button { width: 100%; margin-top: auto; }
.action-panel small { margin-top: 12px; color: #aa9da0; font-size: 11px; text-align: center; }
.action-panel small.error { color: #ff9aaa; }
@media (max-width: 820px) {
  .home-page { padding: 54px 0 64px; }
  .workspace { grid-template-columns: 1fr; }
  .action-panel { min-height: 360px; }
}
@media (max-width: 520px) {
  .hero-copy { margin-bottom: 34px; }
  h1 { font-size: 40px; }
  .hero-copy > p { font-size: 15px; line-height: 1.7; }
}
</style>
