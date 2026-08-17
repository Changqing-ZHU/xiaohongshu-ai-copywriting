<script setup lang="ts">
defineProps<{ modelValue: string }>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <section class="url-card" aria-labelledby="url-input-title">
    <div class="url-heading">
      <span>URL</span>
      <div>
        <h2 id="url-input-title">图片 URL</h2>
        <p>请输入图片链接，例如：https://xxx.com/image.jpg</p>
      </div>
    </div>
    <input
      :value="modelValue"
      type="url"
      inputmode="url"
      autocomplete="url"
      placeholder="https://xxx.com/image.jpg"
      aria-label="图片 URL"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
  </section>
</template>

<style scoped>
.url-card {
  padding: 22px 28px; border: 1px solid var(--line); border-radius: 20px;
  background: var(--card); box-shadow: var(--shadow);
}
.url-heading { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 16px; }
.url-heading > span {
  display: grid; min-width: 42px; height: 34px; place-items: center; border-radius: 10px;
  color: var(--red); font-size: 11px; font-weight: 850; background: var(--soft);
}
h2 { margin: 0 0 5px; color: var(--ink); font-size: 17px; }
p { margin: 0; color: var(--muted); font-size: 12px; }
input {
  width: 100%; padding: 13px 15px; border: 1px solid #e8d9d6; border-radius: 12px;
  outline: none; color: var(--ink); font: inherit; background: #fffbfa;
  transition: border-color 0.2s, box-shadow 0.2s;
}
input:focus { border-color: #ff8998; box-shadow: 0 0 0 3px rgba(255, 67, 87, 0.1); }
input::placeholder { color: #b9a7aa; }
@media (max-width: 700px) { .url-card { padding: 20px; } }
</style>
