import type { AuthSession, RegisteredUser } from '../types/auth'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/$/, '')

export class AuthApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'AuthApiError'
  }
}

const request = async <T>(path: string, options: RequestInit): Promise<T> => {
  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, options)
  } catch {
    throw new AuthApiError('网络连接失败，请检查后端服务后重试。')
  }

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const message = typeof payload?.message === 'string'
      ? payload.message
      : '请求失败，请稍后重试。'
    throw new AuthApiError(message)
  }
  return payload as T
}

const jsonOptions = (body: object): RequestInit => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

export const register = (username: string, password: string) =>
  request<RegisteredUser>('/api/auth/register', jsonOptions({ username, password }))

export const login = (username: string, password: string) =>
  request<AuthSession>('/api/auth/login', jsonOptions({ username, password }))

export const logout = () =>
  request<{ message: string }>('/api/auth/logout', { method: 'POST' })
