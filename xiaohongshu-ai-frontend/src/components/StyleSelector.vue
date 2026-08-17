<script setup lang="ts">
import type { CopywritingStyle } from '../types/generation'

defineProps<{ modelValue: CopywritingStyle }>()

const emit = defineEmits<{
  'update:modelValue': [style: CopywritingStyle]
}>()

const styles: Array<{ value: CopywritingStyle; label: string; description: string }> = [
  { value: 'daily', label: '日常分享', description: '自然松弛，记录生活' },
  { value: 'recommend', label: '种草推荐', description: '突出亮点与使用体验' },
  { value: 'review', label: '专业测评', description: '客观分析优点与不足' },
  { value: 'healing', label: '情绪治愈', description: '温柔共鸣，传递情绪' },
  { value: 'minimal', label: '高级简约', description: '克制精炼，强调质感' },
]
</script>

<template>
  <section class="style-selector" aria-labelledby="style-selector-title">
    <div class="selector-heading">
      <span class="step-label">02</span>
      <div>
        <h2 id="style-selector-title">选择文案风格</h2>
        <p>AI 将按照所选风格组织标题、正文与表达语气</p>
      </div>
    </div>
    <div class="style-options" role="radiogroup" aria-label="文案风格">
      <button
        v-for="item in styles"
        :key="item.value"
        class="style-option"
        :class="{ selected: modelValue === item.value }"
        type="button"
        role="radio"
        :aria-checked="modelValue === item.value"
        @click="emit('update:modelValue', item.value)"
      >
        <span class="selection-dot"></span>
        <strong>{{ item.label }}</strong>
        <small>{{ item.description }}</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
.style-selector {
  margin-top: 18px;
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--card);
  box-shadow: 0 12px 38px rgba(84, 40, 46, 0.06);
}
.selector-heading { display: flex; align-items: center; gap: 14px; }
.step-label {
  display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center;
  border-radius: 11px; color: var(--red); font-size: 12px; font-weight: 800; background: var(--soft);
}
h2 { margin: 0; color: var(--ink); font-size: 18px; }
.selector-heading p { margin: 5px 0 0; color: var(--muted); font-size: 12px; }
.style-options { margin-top: 20px; display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.style-option {
  min-width: 0; padding: 15px 12px; display: grid; justify-items: start; gap: 7px;
  border: 1px solid var(--line); border-radius: 16px; cursor: pointer; text-align: left;
  color: var(--ink); background: #fff; transition: border-color .2s ease, transform .2s ease, background .2s ease;
}
.style-option:hover { transform: translateY(-2px); border-color: #ffc3ca; }
.style-option.selected { border-color: rgba(255, 36, 66, .55); background: var(--soft); box-shadow: inset 0 0 0 1px rgba(255, 36, 66, .08); }
.selection-dot { width: 10px; height: 10px; border: 2px solid #d5c8ca; border-radius: 50%; background: #fff; }
.selected .selection-dot { border-color: var(--red); box-shadow: inset 0 0 0 2px #fff; background: var(--red); }
.style-option strong { overflow: hidden; font-size: 13px; white-space: nowrap; text-overflow: ellipsis; }
.style-option small { color: var(--muted); font-size: 10px; line-height: 1.5; }
@media (max-width: 720px) {
  .style-options { grid-template-columns: repeat(2, 1fr); }
  .style-option:last-child { grid-column: 1 / -1; }
}
@media (max-width: 420px) {
  .style-selector { padding: 20px; }
  .style-options { grid-template-columns: 1fr; }
  .style-option:last-child { grid-column: auto; }
}
</style>
