import type {
  CreatedRequest,
  CreateRequestInput,
  RequestDetail,
  RequestListQuery,
  RequestMutation,
  RequestSummary,
} from '@/types/request'
import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'

export async function createRequest(input: CreateRequestInput): Promise<CreatedRequest> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<CreatedRequest>>(
    '/requests',
    contentPayload(input, true),
  )

  return response.data.data
}

export async function createDraft(input: CreateRequestInput): Promise<CreatedRequest> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<CreatedRequest>>(
    '/requests/drafts',
    contentPayload(input, false),
  )
  return response.data.data
}

export async function updateRequest(
  id: string,
  input: CreateRequestInput,
  expectedVersion: number,
): Promise<RequestMutation> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.put<ApiResponse<RequestMutation>>(
    `/requests/${encodeURIComponent(id)}`,
    { expectedVersion, ...contentPayload(input, false) },
  )
  return response.data.data
}

export async function submitRequest(id: string, expectedVersion: number): Promise<RequestMutation> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<RequestMutation>>(
    `/requests/${encodeURIComponent(id)}/submit`,
    { expectedVersion, informationConfirmed: true },
  )
  return response.data.data
}

export async function cancelRequest(
  id: string,
  expectedVersion: number,
  reason: string,
): Promise<RequestMutation> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<RequestMutation>>(
    `/requests/${encodeURIComponent(id)}/cancel`,
    { expectedVersion, reason: reason.trim() },
  )
  return response.data.data
}

function contentPayload(input: CreateRequestInput, includeConfirmation: boolean) {
  return {
    categoryId: input.categoryId ? Number(input.categoryId) : null,
    title: input.title.trim() || null,
    background: input.background.trim() || null,
    description: input.description.trim() || null,
    expectedResult: input.expectedResult.trim() || null,
    expectedDeadline: input.expectedDeadline || null,
    urgency: input.urgency || null,
    budgetAmount: input.budgetAmount.trim() || null,
    budgetDescription: input.budgetDescription.trim() || null,
    technicalConstraints: input.technicalConstraints.trim() || null,
    contactInfo: input.contactInfo.trim() || null,
    ...(includeConfirmation ? { informationConfirmed: input.informationConfirmed } : {}),
  }
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
