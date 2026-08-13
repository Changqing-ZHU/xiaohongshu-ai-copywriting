<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Navbar from './components/Navbar.vue'
import HomeView from './views/HomeView.vue'
import ResultView from './views/ResultView.vue'
import {
  ApiRequestError,
  createGeneration,
  getGeneration,
  uploadGenerationImage,
} from './services/generationApi'
import type { GeneratedDraft, GenerationInput, GenerationResponse } from './types/generation'

const pollIntervalMs = 1200
const maxPollAttempts = 60

const currentPath = ref(window.location.pathname)
const generatedDraft = ref<GeneratedDraft | null>(null)
let activeController: AbortController | null = null
let activeOperation = 0

const syncPath = () => {
  currentPath.value = window.location.pathname
  if (currentPath.value !== '/result') activeController?.abort()
}

const navigate = (path: string) => {
  if (window.location.pathname !== path) {
    window.history.pushState({}, '', path)
  }
  currentPath.value = path
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const updateFromResponse = (response: GenerationResponse, operation: number) => {
  if (operation !== activeOperation || !generatedDraft.value) return

  generatedDraft.value = {
    ...generatedDraft.value,
    generationId: response.id,
    status: response.status,
    imageAnalysis: response.imageAnalysis,
    title: response.title,
    content: response.content,
    tags: response.tags,
    errorMessage: response.errorMessage,
    failureType: response.status === 'FAILED' ? 'AI' : null,
  }
}

const waitForNextPoll = (signal: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    const onAbort = () => {
      window.clearTimeout(timeoutId)
      reject(new DOMException('Generation cancelled', 'AbortError'))
    }
    const timeoutId = window.setTimeout(() => {
      signal.removeEventListener('abort', onAbort)
      resolve()
    }, pollIntervalMs)
    signal.addEventListener('abort', onAbort, { once: true })
  })

const pollGeneration = async (id: number, signal: AbortSignal, operation: number) => {
  for (let attempt = 0; attempt < maxPollAttempts; attempt += 1) {
    const response = await getGeneration(id, signal)
    updateFromResponse(response, operation)

    if (response.status === 'COMPLETED' || response.status === 'FAILED') return
    await waitForNextPoll(signal)
  }

  throw new Error('生成时间较长，请稍后重新查询或重试')
}

const startGeneration = async (input: GenerationInput) => {
  activeController?.abort()
  const controller = new AbortController()
  activeController = controller
  const operation = ++activeOperation

  generatedDraft.value = {
    generationId: null,
    imageUrl: input.imageUrl,
    fileName: input.fileName,
    fileSize: input.fileSize,
    status: 'PROCESSING',
    imageAnalysis: null,
    title: null,
    content: null,
    tags: [],
    errorMessage: null,
    failureType: null,
  }
  navigate('/result')

  try {
    const created = await createGeneration(controller.signal)
    if (operation !== activeOperation || !generatedDraft.value) return
    generatedDraft.value = {
      ...generatedDraft.value,
      generationId: created.id,
      status: created.status,
    }

    try {
      await uploadGenerationImage(created.id, input.file, controller.signal)
    } catch (uploadError) {
      // AI failures return an HTTP error after the backend has persisted FAILED.
      const response = await getGeneration(created.id, controller.signal)
      updateFromResponse(response, operation)
      if (response.status === 'FAILED') return
      throw uploadError
    }

    await pollGeneration(created.id, controller.signal, operation)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    if (operation !== activeOperation || !generatedDraft.value) return

    generatedDraft.value = {
      ...generatedDraft.value,
      status: 'FAILED',
      errorMessage: error instanceof Error ? error.message : '生成失败，请稍后重试',
      failureType: error instanceof ApiRequestError ? error.kind : 'OTHER',
    }
  } finally {
    if (operation === activeOperation) activeController = null
  }
}

const restart = () => {
  activeController?.abort()
  activeController = null
  activeOperation += 1
  generatedDraft.value = null
  navigate('/')
}

onMounted(() => window.addEventListener('popstate', syncPath))
onBeforeUnmount(() => {
  activeController?.abort()
  window.removeEventListener('popstate', syncPath)
})
</script>

<template>
  <div class="app-shell">
    <Navbar :active-path="currentPath" @navigate="restart" />
    <main>
      <ResultView
        v-if="currentPath === '/result'"
        :draft="generatedDraft"
        @restart="restart"
      />
      <HomeView v-else @generate="startGeneration" />
    </main>
  </div>
</template>
