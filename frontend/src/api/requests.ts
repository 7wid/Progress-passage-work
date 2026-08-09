import type {
  CreatedRequest,
  CreateRequestInput,
  RequestDetail,
  RequestListQuery,
  RequestSummary,
} from '@/types/request'
import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'

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
export async function getRequests(query: RequestListQuery): Promise<PageResponse<RequestSummary>> {
  const response = await http.get<ApiResponse<PageResponse<RequestSummary>>>('/requests', {
    params: {
      ...query,
      keyword: query.keyword?.trim() || undefined,
      categoryId: query.categoryId || undefined,
      submittedFrom: query.submittedFrom || undefined,
      submittedTo: query.submittedTo || undefined,
    },
  })

  return response.data.data
}

export async function getRequestDetail(id: string): Promise<RequestDetail> {
  const response = await http.get<ApiResponse<RequestDetail>>(`/requests/${encodeURIComponent(id)}`)

  return response.data.data
}
