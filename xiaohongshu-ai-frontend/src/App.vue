<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Navbar from './components/Navbar.vue'
import HomeView from './views/HomeView.vue'
import ResultView from './views/ResultView.vue'
import type { GeneratedDraft } from './types/generation'

const currentPath = ref(window.location.pathname)
const generatedDraft = ref<GeneratedDraft | null>(null)

const syncPath = () => {
  currentPath.value = window.location.pathname
}

const navigate = (path: string) => {
  if (window.location.pathname !== path) {
    window.history.pushState({}, '', path)
  }
  currentPath.value = path
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const showResult = (draft: GeneratedDraft) => {
  generatedDraft.value = draft
  navigate('/result')
}

onMounted(() => window.addEventListener('popstate', syncPath))
onBeforeUnmount(() => window.removeEventListener('popstate', syncPath))
</script>

<template>
  <div class="app-shell">
    <Navbar :active-path="currentPath" @navigate="navigate" />
    <main>
      <ResultView
        v-if="currentPath === '/result'"
        :draft="generatedDraft"
        @restart="navigate('/')"
      />
      <HomeView v-else @generated="showResult" />
    </main>
  </div>
</template>
