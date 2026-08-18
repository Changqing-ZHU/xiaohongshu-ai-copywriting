import { computed, ref } from 'vue'
import type { AuthSession } from '../types/auth'

const TOKEN_KEY = 'token'
const USERNAME_KEY = 'username'
const ROLE_KEY = 'role'

const readStorage = (key: string) => {
  try {
    return window.localStorage.getItem(key) ?? ''
  } catch {
    return ''
  }
}

const writeStorage = (key: string, value: string) => {
  try {
    window.localStorage.setItem(key, value)
  } catch {
    // Reactive state still supports the current session if storage is unavailable.
  }
}

const removeStorage = (key: string) => {
  try {
    window.localStorage.removeItem(key)
  } catch {
    // Local reactive state is cleared even if storage is unavailable.
  }
}

export const authToken = ref(readStorage(TOKEN_KEY))
export const authUsername = ref(readStorage(USERNAME_KEY))
export const authRole = ref(readStorage(ROLE_KEY))
export const isAuthenticated = computed(() => Boolean(authToken.value))

export const saveAuthSession = (session: AuthSession) => {
  authToken.value = session.token
  authUsername.value = session.username
  authRole.value = session.role
  writeStorage(TOKEN_KEY, session.token)
  writeStorage(USERNAME_KEY, session.username)
  writeStorage(ROLE_KEY, session.role)
}

export const clearAuthSession = () => {
  authToken.value = ''
  authUsername.value = ''
  authRole.value = ''
  removeStorage(TOKEN_KEY)
  removeStorage(USERNAME_KEY)
  removeStorage(ROLE_KEY)
}

export const getToken = () => authToken.value
