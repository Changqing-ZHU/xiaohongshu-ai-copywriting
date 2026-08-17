<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Navbar from './components/Navbar.vue'
import HomeView from './views/HomeView.vue'
import HistoryView from './views/HistoryView.vue'
import LandingView from './views/LandingView.vue'
import ResultView from './views/ResultView.vue'
import {
  ApiRequestError,
  createGeneration,
  getGeneration,
  resolveApiUrl,
  triggerGeneration,
  uploadGenerationImage,
} from './services/generationApi'
import type {
  GeneratedDraft,
  GenerationHistoryItem,
  GenerationInput,
  GenerationResponse,
} from './types/generation'

const pollIntervalMs = 1200
const maxPollAttempts = 60

const classifyFailure = (message: string | null) => {
  if (!message) return 'AI' as const
  if (message.includes('smaller than 10MB')) return 'IMAGE_SIZE' as const
  if (message.includes('Unsupported image URL format')) return 'IMAGE_FORMAT' as const
  if (message.includes('image URL')) return 'URL_ACCESS' as const
  return 'AI' as const
}

const currentPath = ref(window.location.pathname)
const generatedDraft = ref<GeneratedDraft | null>(null)
const resultReturnPath = ref('/workbench')
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
    failureType: response.status === 'FAILED'
      ? classifyFailure(response.errorMessage)
      : null,
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
  resultReturnPath.value = '/workbench'

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
    sourceUrl: input.url,
  }
  navigate('/result')

  try {
    const created = await createGeneration(input.url, input.style, controller.signal)
    if (operation !== activeOperation || !generatedDraft.value) return
    generatedDraft.value = {
      ...generatedDraft.value,
      generationId: created.id,
      status: created.status,
    }

    if (input.file) {
      try {
        await uploadGenerationImage(created.id, input.file, controller.signal)
      } catch (uploadError) {
        // AI failures return an HTTP error after the backend has persisted FAILED.
        const response = await getGeneration(created.id, controller.signal)
        updateFromResponse(response, operation)
        if (response.status === 'FAILED') return
        throw uploadError
      }
    } else {
      await triggerGeneration(created.id, controller.signal)
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

const openHistoryRecord = async (record: GenerationHistoryItem) => {
  activeController?.abort()
  const controller = new AbortController()
  activeController = controller
  const operation = ++activeOperation
  resultReturnPath.value = '/history'
  generatedDraft.value = {
    generationId: record.id,
    imageUrl: record.imageUrl ? resolveApiUrl(record.imageUrl) : '',
    fileName: record.title || `历史记录 #${record.id}`,
    fileSize: 0,
    status: record.status,
    imageAnalysis: null,
    title: record.title,
    content: record.content,
    tags: record.tags,
    errorMessage: null,
    failureType: record.status === 'FAILED' ? 'AI' : null,
    sourceUrl: '',
  }
  navigate('/result')

  try {
    const response = await getGeneration(record.id, controller.signal)
    updateFromResponse(response, operation)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    if (operation !== activeOperation || !generatedDraft.value) return
    generatedDraft.value = {
      ...generatedDraft.value,
      status: 'FAILED',
      errorMessage: error instanceof Error ? error.message : '历史详情加载失败，请稍后重试。',
      failureType: error instanceof ApiRequestError ? error.kind : 'OTHER',
    }
  } finally {
    if (operation === activeOperation) activeController = null
  }
}

const handleNavigation = (path: string) => {
  activeController?.abort()
  activeController = null
  activeOperation += 1
  generatedDraft.value = null
  navigate(path)
}

const leaveResult = () => handleNavigation(resultReturnPath.value)

onMounted(() => window.addEventListener('popstate', syncPath))
onBeforeUnmount(() => {
  activeController?.abort()
  window.removeEventListener('popstate', syncPath)
})
</script>

<template>
  <div class="app-shell">
    <Navbar :active-path="currentPath" @navigate="handleNavigation" />
    <main>
      <ResultView
        v-if="currentPath === '/result'"
        :draft="generatedDraft"
        :return-label="resultReturnPath === '/history' ? '返回历史记录' : '返回重新生成'"
        @restart="leaveResult"
      />
      <HistoryView
        v-else-if="currentPath === '/history'"
        @select="openHistoryRecord"
      />
      <HomeView v-else-if="currentPath === '/workbench'" @generate="startGeneration" />
      <LandingView v-else @start="handleNavigation('/workbench')" />
    </main>
  </div>
</template>
