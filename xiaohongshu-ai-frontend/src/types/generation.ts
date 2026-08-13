export type GenerationStatus = 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type GenerationFailureType = 'IMAGE_SIZE' | 'IMAGE_FORMAT' | 'AI' | 'NETWORK' | 'OTHER'

export interface GenerationInput {
  file: File
  imageUrl: string
  fileName: string
  fileSize: number
}

export interface GeneratedDraft {
  generationId: number | null
  imageUrl: string
  fileName: string
  fileSize: number
  status: GenerationStatus
  imageAnalysis: string | null
  title: string | null
  content: string | null
  tags: string[]
  errorMessage: string | null
  failureType: GenerationFailureType | null
}

export interface GenerationCreatedResponse {
  id: number
  status: GenerationStatus
  createdAt: string
}

export interface GenerationImageUploadedResponse {
  id: number
  status: GenerationStatus
}

export interface GenerationResponse {
  id: number
  status: GenerationStatus
  createdAt: string
  updatedAt: string
  imageAnalysis: string | null
  title: string | null
  content: string | null
  tags: string[]
  errorMessage: string | null
}
