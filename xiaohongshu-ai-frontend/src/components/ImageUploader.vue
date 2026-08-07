<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ file: File | null; previewUrl: string }>()

const emit = defineEmits<{
  select: [file: File]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const openFilePicker = () => inputRef.value?.click()

const selectFile = (files: FileList | null) => {
  const file = files?.[0]
  if (file?.type.startsWith('image/')) emit('select', file)
}

const onDrop = (event: DragEvent) => selectFile(event.dataTransfer?.files ?? null)

const formatSize = (bytes: number) => {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
  <section class="uploader-card" aria-labelledby="upload-title">
    <div class="section-heading">
      <span>01</span>
      <div>
        <h2 id="upload-title">选择一张图片</h2>
        <p>支持 JPG、PNG、WebP 等常见图片格式</p>
      </div>
    </div>
    <div class="upload-zone" :class="{ 'has-image': file }" @dragover.prevent @drop.prevent="onDrop">
      <template v-if="file">
        <img :src="previewUrl" :alt="`已选择图片：${file.name}`" />
        <div class="file-detail">
          <div class="success-icon">✓</div>
          <div class="file-copy">
            <strong>{{ file.name }}</strong>
            <span>{{ formatSize(file.size) }} · 图片已准备好</span>
          </div>
          <button class="change-button" type="button" @click="openFilePicker">重新选择</button>
        </div>
      </template>
      <button v-else class="empty-upload" type="button" @click="openFilePicker">
        <span class="upload-icon">＋</span>
        <strong>点击上传图片</strong>
        <span>也可以将图片拖放到这里</span>
      </button>
      <input
        ref="inputRef"
        class="sr-only"
        type="file"
        accept="image/*"
        @change="selectFile(($event.target as HTMLInputElement).files)"
      />
    </div>
  </section>
</template>

<style scoped>
.uploader-card {
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--card);
  box-shadow: var(--shadow);
}
.section-heading { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 22px; }
.section-heading > span {
  display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center;
  border-radius: 10px; color: var(--red); font-size: 12px; font-weight: 800; background: var(--soft);
}
h2 { margin: 0 0 5px; color: var(--ink); font-size: 19px; }
p { margin: 0; color: var(--muted); font-size: 13px; }
.upload-zone {
  min-height: 300px; overflow: hidden; border: 1.5px dashed #e4cdca;
  border-radius: 18px; background: #fffbfa;
}
.empty-upload {
  width: 100%; min-height: 300px; display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 10px; border: 0; cursor: pointer; color: var(--muted); background: transparent;
}
.empty-upload strong { color: var(--ink); font-size: 16px; }
.empty-upload > span:last-child { font-size: 13px; }
.upload-icon {
  display: grid; width: 54px; height: 54px; margin-bottom: 4px; place-items: center;
  border-radius: 17px; color: var(--red); font-size: 28px; background: var(--soft);
}
.has-image img { display: block; width: 100%; height: 300px; object-fit: contain; background: #f7f1ef; }
.file-detail { min-height: 72px; padding: 14px 16px; display: flex; align-items: center; gap: 12px; background: #fff; }
.success-icon {
  display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center;
  border-radius: 50%; color: #168866; font-weight: 800; background: #e9f8f1;
}
.file-copy { min-width: 0; display: flex; flex: 1; flex-direction: column; gap: 3px; }
.file-copy strong { overflow: hidden; color: var(--ink); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.file-copy span { color: var(--muted); font-size: 12px; }
.change-button {
  flex: 0 0 auto; padding: 8px 12px; border: 0; border-radius: 10px; cursor: pointer;
  color: var(--red); font-size: 13px; font-weight: 700; background: var(--soft);
}
.sr-only {
  position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px;
  overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0;
}
@media (max-width: 700px) {
  .uploader-card { padding: 20px; }
  .upload-zone, .empty-upload { min-height: 240px; }
  .has-image img { height: 240px; }
}
</style>
