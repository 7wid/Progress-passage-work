import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RequestSummary } from '@/types/request'

export async function getRequests(page = 1, pageSize = 20): Promise<PageResponse<RequestSummary>> {
  const response = await http.get<ApiResponse<PageResponse<RequestSummary>>>('/requests', {
    params: { page, pageSize },
  })
  return response.data.data
}
