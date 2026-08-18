export interface AdminDashboard {
  totalUsers: number
  totalGenerations: number
  todayGenerations: number
  todayActiveUsers: number
}

export interface AdminUser {
  username: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface AdminGeneration {
  id: number
  username: string | null
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  imageUrl: string | null
  originalFileName: string | null
  imageContentType: string | null
  imageSize: number | null
  title: string | null
  createdAt: string
}
