import type {
  GenerationCreatedResponse,
  GenerationImageUploadedResponse,
  GenerationResponse,
} from '../types/generation'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/$/, '')

const request = async <T>(path: string, options: RequestInit): Promise<T> => {
  const response = await fetch(`${apiBaseUrl}${path}`, options)
  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    const message = typeof payload?.message === 'string' ? payload.message : '请求失败，请稍后重试'
    throw new Error(message)
  }

  return payload as T
}

export const createGeneration = (signal: AbortSignal) =>
  request<GenerationCreatedResponse>('/api/generations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}',
    signal,
  })

export const uploadGenerationImage = (id: number, image: File, signal: AbortSignal) => {
  const formData = new FormData()
  formData.append('image', image)

  return request<GenerationImageUploadedResponse>(`/api/generations/${id}/image`, {
    method: 'POST',
    body: formData,
    signal,
  })
}

export const getGeneration = (id: number, signal: AbortSignal) =>
  request<GenerationResponse>(`/api/generations/${id}`, {
    method: 'GET',
    signal,
  })
