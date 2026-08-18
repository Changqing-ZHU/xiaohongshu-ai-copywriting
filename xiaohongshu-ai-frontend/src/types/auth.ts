export interface AuthSession {
  token: string
  username: string
  role: string
}

export interface RegisteredUser {
  id: number
  username: string
  role: string
  createdAt: string
}
