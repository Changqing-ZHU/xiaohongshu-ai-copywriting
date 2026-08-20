<script setup lang="ts">
import type {
  AgeGroup,
  ContentType,
  CopyLength,
  EmojiPreference,
  RecommendationLevel,
  TargetAudience,
} from '../types/generation'

defineProps<{
  contentType: ContentType
  targetAudience: TargetAudience
  ageGroup: AgeGroup
  recommendationLevel: RecommendationLevel
  copyLength: CopyLength
  emojiPreference: EmojiPreference
}>()

const emit = defineEmits<{
  'update:contentType': [value: ContentType]
  'update:targetAudience': [value: TargetAudience]
  'update:ageGroup': [value: AgeGroup]
  'update:recommendationLevel': [value: RecommendationLevel]
  'update:copyLength': [value: CopyLength]
  'update:emojiPreference': [value: EmojiPreference]
}>()

const contentTypes: Array<{ value: ContentType; label: string }> = [
  { value: 'daily_record', label: '日常记录' },
  { value: 'food', label: '美食探店' },
  { value: 'travel', label: '旅行打卡' },
  { value: 'outfit', label: '穿搭分享' },
  { value: 'product_recommendation', label: '好物推荐' },
  { value: 'product_review', label: '产品测评' },
  { value: 'beauty', label: '美妆护肤' },
  { value: 'home', label: '家居生活' },
  { value: 'digital', label: '数码科技' },
  { value: 'learning', label: '学习成长' },
]

const audiences: Array<{ value: TargetAudience; label: string }> = [
  { value: 'students', label: '学生党' },
  { value: 'young_women', label: '年轻女性' },
  { value: 'professionals', label: '职场人士' },
  { value: 'mothers', label: '宝妈群体' },
  { value: 'couples', label: '情侣用户' },
  { value: 'general', label: '大众用户' },
]

const ageGroups: Array<{ value: AgeGroup; label: string }> = [
  { value: 'under_18', label: '18岁以下' },
  { value: '18_25', label: '18-25岁' },
  { value: '25_35', label: '25-35岁' },
  { value: '35_plus', label: '35岁以上' },
  { value: 'unrestricted', label: '不限定' },
]

const recommendationLevels: Array<{ value: RecommendationLevel; label: string }> = [
  { value: 'share', label: '纯分享' },
  { value: 'light', label: '轻度种草' },
  { value: 'strong', label: '明显推荐' },
  { value: 'marketing', label: '强营销推广' },
]

const copyLengths: Array<{ value: CopyLength; label: string }> = [
  { value: 'short', label: '简短版' },
  { value: 'standard', label: '标准版' },
  { value: 'detailed', label: '详细版' },
]

const emojiPreferences: Array<{ value: EmojiPreference; label: string }> = [
  { value: 'none', label: '不使用' },
  { value: 'few', label: '少量使用' },
  { value: 'rich', label: '丰富使用' },
]
</script>

<template>
  <section class="options-panel" aria-labelledby="generation-options-title">
    <div class="panel-heading">
      <span class="step-label">03</span>
      <div>
        <h2 id="generation-options-title">完善创作意图</h2>
        <p>这些选择会直接用于 AI 文案创作</p>
      </div>
    </div>

    <div class="option-grid">
      <label>
        <span>内容类型</span>
        <select :value="contentType" @change="emit('update:contentType', ($event.target as HTMLSelectElement).value as ContentType)">
          <option v-for="item in contentTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>目标受众</span>
        <select :value="targetAudience" @change="emit('update:targetAudience', ($event.target as HTMLSelectElement).value as TargetAudience)">
          <option v-for="item in audiences" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>年龄段</span>
        <select :value="ageGroup" @change="emit('update:ageGroup', ($event.target as HTMLSelectElement).value as AgeGroup)">
          <option v-for="item in ageGroups" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>推荐程度</span>
        <select :value="recommendationLevel" @change="emit('update:recommendationLevel', ($event.target as HTMLSelectElement).value as RecommendationLevel)">
          <option v-for="item in recommendationLevels" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>文案长度</span>
        <select :value="copyLength" @change="emit('update:copyLength', ($event.target as HTMLSelectElement).value as CopyLength)">
          <option v-for="item in copyLengths" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>Emoji 偏好</span>
        <select :value="emojiPreference" @change="emit('update:emojiPreference', ($event.target as HTMLSelectElement).value as EmojiPreference)">
          <option v-for="item in emojiPreferences" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
    </div>
  </section>
</template>

<style scoped>
.options-panel {
  margin-top: 18px; padding: 26px; border: 1px solid var(--line);
  border-radius: 24px; background: var(--card); box-shadow: 0 12px 38px rgba(84, 40, 46, 0.06);
}
.panel-heading { display: flex; align-items: center; gap: 14px; }
.step-label {
  display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center;
  border-radius: 11px; color: var(--red); font-size: 12px; font-weight: 800; background: var(--soft);
}
h2 { margin: 0; color: var(--ink); font-size: 18px; }
.panel-heading p { margin: 5px 0 0; color: var(--muted); font-size: 12px; }
.option-grid { margin-top: 20px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
label { display: grid; gap: 7px; }
label > span { color: #705f64; font-size: 12px; font-weight: 700; }
select {
  width: 100%; min-height: 44px; padding: 0 36px 0 13px; border: 1px solid var(--line);
  border-radius: 13px; outline: none; color: var(--ink); font: inherit; font-size: 13px;
  background: #fff; cursor: pointer;
}
select:focus { border-color: #f19da8; box-shadow: 0 0 0 3px #fff0f2; }
@media (max-width: 720px) { .option-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 420px) {
  .options-panel { padding: 20px; }
  .option-grid { grid-template-columns: 1fr; }
}
</style>
