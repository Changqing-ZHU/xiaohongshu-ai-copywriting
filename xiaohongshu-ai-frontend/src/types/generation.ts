export type GenerationStatus = 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type CopywritingStyle =
  | 'daily'
  | 'recommend'
  | 'review'
  | 'healing'
  | 'minimal'
  | 'viral'
  | 'authentic'
  | 'tutorial'
export type ContentType =
  | 'daily_record'
  | 'food'
  | 'travel'
  | 'outfit'
  | 'product_recommendation'
  | 'product_review'
  | 'beauty'
  | 'home'
  | 'digital'
  | 'learning'
export type TargetAudience =
  | 'students'
  | 'young_women'
  | 'professionals'
  | 'mothers'
  | 'couples'
  | 'general'
export type AgeGroup = 'under_18' | '18_25' | '25_35' | '35_plus' | 'unrestricted'
export type RecommendationLevel = 'share' | 'light' | 'strong' | 'marketing'
export type CopyLength = 'short' | 'standard' | 'detailed'
export type EmojiPreference = 'none' | 'few' | 'rich'
export type GenerationFailureType =
  | 'IMAGE_SIZE'
  | 'IMAGE_FORMAT'
  | 'URL_FORMAT'
  | 'URL_ACCESS'
  | 'AI'
  | 'NETWORK'
  | 'OTHER'

export interface GenerationInput {
  file: File | null
  imageUrl: string
  fileName: string
  fileSize: number
  url: string
  style: CopywritingStyle
  scene: ContentType
  audience: TargetAudience
  ageGroup: AgeGroup
  marketingLevel: RecommendationLevel
  length: CopyLength
  emojiPreference: EmojiPreference
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
  sourceUrl: string
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

export interface GenerationProcessingResponse {
  id: number
  status: GenerationStatus
}

export interface GenerationResponse {
  id: number
  status: GenerationStatus
  imageUrl: string | null
  imagePath?: string | null
  createdAt: string
  updatedAt: string
  imageAnalysis: string | null
  title: string | null
  content: string | null
  tags: string[]
  errorMessage: string | null
}

export interface GenerationHistoryItem {
  id: number
  status: GenerationStatus
  imageUrl: string | null
  title: string | null
  content: string | null
  tags: string[]
  createdAt: string
}
