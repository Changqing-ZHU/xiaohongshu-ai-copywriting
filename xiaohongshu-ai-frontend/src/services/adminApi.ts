import { getToken } from '../stores/authStore'
import type { AdminDashboard, AdminGeneration, AdminUser } from '../types/admin'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/$/, '')

const get = async <T>(path: string, signal?: AbortSignal): Promise<T> => {
  const headers = new Headers()
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, { method: 'GET', headers, signal })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new Error('网络连接失败，请检查后端服务。')
  }

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(typeof payload?.message === 'string'
      ? payload.message
      : '后台数据加载失败。')
  }
  return payload as T
}

export const getAdminDashboard = (signal?: AbortSignal) =>
  get<AdminDashboard>('/api/admin/dashboard', signal)

export const getAdminUsers = (signal?: AbortSignal) =>
  get<AdminUser[]>('/api/admin/users', signal)

export const getAdminGenerations = (signal?: AbortSignal) =>
  get<AdminGeneration[]>('/api/admin/generations', signal)
