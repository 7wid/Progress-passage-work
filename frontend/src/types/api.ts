export interface ApiResponse<T> {
  data: T
  requestId: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface ApiErrorPayload {
  error: {
    code: string
    message: string
    details?: Array<{ field?: string; message: string }>
  }
  requestId: string
}
