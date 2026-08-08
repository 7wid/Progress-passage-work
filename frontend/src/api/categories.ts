import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { CategoryOption } from '@/types/request'

export async function getEnabledCategories(): Promise<CategoryOption[]> {
  const response = await http.get<ApiResponse<CategoryOption[]>>('/categories')

  return response.data.data
}
