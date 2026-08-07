<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  eyebrow: string
  title?: string
  content?: string
  tags?: string[]
}>()

const copied = ref(false)
const copyText = computed(() =>
  props.tags ? props.tags.map((tag) => `#${tag}`).join(' ') : [props.title, props.content].filter(Boolean).join('\n'),
)

const copy = async () => {
  await navigator.clipboard.writeText(copyText.value)
  copied.value = true
  window.setTimeout(() => (copied.value = false), 1600)
}
</script>

<template>
  <article class="copy-card">
    <div class="card-topline">
      <span>{{ eyebrow }}</span>
      <button class="copy-button" type="button" @click="copy">{{ copied ? '已复制' : '复制' }}</button>
    </div>
    <h3 v-if="title">{{ title }}</h3>
    <p v-if="content">{{ content }}</p>
    <div v-if="tags" class="tag-list">
      <span v-for="tag in tags" :key="tag"># {{ tag }}</span>
    </div>
  </article>
</template>

<style scoped>
.copy-card { padding: 22px; border: 1px solid var(--line); border-radius: 18px; background: #fff; }
.card-topline { margin-bottom: 14px; display: flex; align-items: center; justify-content: space-between; }
.card-topline > span { color: #a07178; font-size: 12px; font-weight: 800; letter-spacing: 0.12em; }
.copy-button { padding: 7px 12px; color: var(--red); font-size: 12px; font-weight: 750; background: var(--soft); }
h3 { margin: 0; color: var(--ink); font-size: 21px; line-height: 1.45; }
p { margin: 0; color: #554a4e; font-size: 15px; line-height: 1.9; white-space: pre-line; }
.tag-list { display: flex; flex-wrap: wrap; gap: 9px; }
.tag-list span {
  padding: 8px 12px; border-radius: 999px; color: var(--red);
  font-size: 13px; font-weight: 650; background: var(--soft);
}
</style>
