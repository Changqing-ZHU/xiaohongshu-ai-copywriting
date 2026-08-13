import type {
  GenerationCreatedResponse,
  GenerationImageUploadedResponse,
  GenerationResponse,
} from '../types/generation'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/$/, '')

export type ApiErrorKind = 'IMAGE_SIZE' | 'IMAGE_FORMAT' | 'AI' | 'NETWORK' | 'OTHER'

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
    if (response.status === 413) kind = 'IMAGE_SIZE'
    else if (response.status === 400 && path.endsWith('/image')) kind = 'IMAGE_FORMAT'
    else if (message === 'Unable to generate copywriting') kind = 'AI'
    throw new ApiRequestError(message, kind)
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
