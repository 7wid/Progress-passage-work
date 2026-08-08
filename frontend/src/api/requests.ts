import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { CreatedRequest, CreateRequestInput, RequestSummary } from '@/types/request'

export async function createRequest(input: CreateRequestInput): Promise<CreatedRequest> {
  await http.get<ApiResponse<string>>('/auth/csrf')

  const payload = {
    ...input,
    categoryId: Number(input.categoryId),
    budgetAmount: input.budgetAmount.trim() || null,
    budgetDescription: input.budgetDescription.trim() || null,
    technicalConstraints: input.technicalConstraints.trim() || null,
  }

  const response = await http.post<ApiResponse<CreatedRequest>>('/requests', payload)

  return response.data.data
}

export async function getRequests(page = 1, pageSize = 20): Promise<PageResponse<RequestSummary>> {
  const response = await http.get<ApiResponse<PageResponse<RequestSummary>>>('/requests', {
    params: { page, pageSize },
  })

  return response.data.data
}
