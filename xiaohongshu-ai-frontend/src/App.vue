<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Navbar from './components/Navbar.vue'
import AdminView from './views/AdminView.vue'
import AdminGenerationsView from './views/AdminGenerationsView.vue'
import AdminUsersView from './views/AdminUsersView.vue'
import HomeView from './views/HomeView.vue'
import HistoryView from './views/HistoryView.vue'
import LandingView from './views/LandingView.vue'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import ResultView from './views/ResultView.vue'
import { logout as logoutRequest } from './services/authApi'
import {
  authUsername,
  authRole,
  clearAuthSession,
  isAuthenticated,
} from './stores/authStore'
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

const protectedPaths = new Set(['/workbench', '/history', '/result'])
const guardPath = (path: string) => {
  const isAdminPath = path === '/admin' || path.startsWith('/admin/')
  if ((protectedPaths.has(path) || isAdminPath) && !isAuthenticated.value) return '/login'
  if (isAdminPath && authRole.value !== 'ADMIN') return '/workbench'
  return path
}
const initialPath = guardPath(window.location.pathname)
if (initialPath !== window.location.pathname) {
  window.history.replaceState({}, '', initialPath)
}

const currentPath = ref(initialPath)
const generatedDraft = ref<GeneratedDraft | null>(null)
const resultReturnPath = ref('/workbench')
let activeController: AbortController | null = null
let activeOperation = 0

const syncPath = () => {
  const guardedPath = guardPath(window.location.pathname)
  if (guardedPath !== window.location.pathname) {
    window.history.replaceState({}, '', guardedPath)
  }
  currentPath.value = guardedPath
  if (currentPath.value !== '/result') activeController?.abort()
}

const navigate = (path: string) => {
  const guardedPath = guardPath(path)
  if (window.location.pathname !== guardedPath) {
    window.history.pushState({}, '', guardedPath)
  }
  currentPath.value = guardedPath
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const updateFromResponse = (response: GenerationResponse, operation: number) => {
  if (operation !== activeOperation || !generatedDraft.value) return

  const storedImageUrl = response.imageUrl
    || (response.imagePath ? `/api/generations/${response.id}/image` : '')

  generatedDraft.value = {
    ...generatedDraft.value,
    generationId: response.id,
    imageUrl: storedImageUrl
      ? resolveApiUrl(storedImageUrl)
      : generatedDraft.value.imageUrl,
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

const handleAuthenticated = () => handleNavigation('/workbench')
const handleRegistered = () => handleNavigation('/login')

const handleLogout = async () => {
  try {
    await logoutRequest()
  } catch {
    // Stateless logout is completed locally even if the acknowledgement fails.
  } finally {
    clearAuthSession()
    handleNavigation('/login')
  }
}

onMounted(() => window.addEventListener('popstate', syncPath))
onBeforeUnmount(() => {
  activeController?.abort()
  window.removeEventListener('popstate', syncPath)
})
</script>

<template>
  <div class="app-shell">
    <Navbar
      :active-path="currentPath"
      :authenticated="isAuthenticated"
      :username="authUsername"
      :role="authRole"
      @navigate="handleNavigation"
      @logout="handleLogout"
    />
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
      <AdminView v-else-if="currentPath === '/admin'" @navigate="handleNavigation" />
      <AdminUsersView v-else-if="currentPath === '/admin/users'" @navigate="handleNavigation" />
      <AdminGenerationsView v-else-if="currentPath === '/admin/generations'" @navigate="handleNavigation" />
      <HomeView v-else-if="currentPath === '/workbench'" @generate="startGeneration" />
      <Login
        v-else-if="currentPath === '/login'"
        @authenticated="handleAuthenticated"
        @navigate="handleNavigation"
      />
      <Register
        v-else-if="currentPath === '/register'"
        @registered="handleRegistered"
        @navigate="handleNavigation"
      />
      <LandingView v-else @start="handleNavigation('/workbench')" />
    </main>
  </div>
</template>
