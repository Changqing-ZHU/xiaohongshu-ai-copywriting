import type {
  CopywritingStyle,
  GenerationCreatedResponse,
  GenerationHistoryItem,
  GenerationImageUploadedResponse,
  GenerationProcessingResponse,
  GenerationResponse,
} from '../types/generation'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/$/, '')

export const resolveApiUrl = (path: string) => {
  if (/^https?:\/\//i.test(path)) return path
  return `${apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`
}

export type ApiErrorKind =
  | 'IMAGE_SIZE'
  | 'IMAGE_FORMAT'
  | 'URL_ACCESS'
  | 'AI'
  | 'NETWORK'
  | 'OTHER'

export class ApiRequestError extends Error {
  readonly kind: ApiErrorKind

  constructor(message: string, kind: ApiErrorKind) {
    super(message)
    this.name = 'ApiRequestError'
    this.kind = kind
  }
}

const request = async <T>(path: string, options: RequestInit): Promise<T> => {
  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, options)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiRequestError('网络连接失败，请检查网络或后端服务后重试。', 'NETWORK')
  }

  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    const message = typeof payload?.message === 'string' ? payload.message : '请求失败，请稍后重试'
    let kind: ApiErrorKind = 'OTHER'
    if (response.status === 413 || message.includes('smaller than 10MB')) kind = 'IMAGE_SIZE'
    else if (response.status === 400 && path.endsWith('/image')) kind = 'IMAGE_FORMAT'
    else if (message.includes('Unsupported image URL format')) kind = 'IMAGE_FORMAT'
    else if (message.includes('image URL')) kind = 'URL_ACCESS'
    else if (message === 'Unable to generate copywriting') kind = 'AI'
    throw new ApiRequestError(message, kind)
  }

  return payload as T
}

export const createGeneration = (url: string, style: CopywritingStyle, signal: AbortSignal) =>
  request<GenerationCreatedResponse>('/api/generations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url: url.trim() || null, style }),
    signal,
  })

export const triggerGeneration = (id: number, signal: AbortSignal) =>
  request<GenerationProcessingResponse>(`/api/generations/${id}/generate`, {
    method: 'POST',
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

export const getGenerationHistory = (signal: AbortSignal) =>
  request<GenerationHistoryItem[]>('/api/generations', {
    method: 'GET',
    signal,
  })
