import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  AdminRequester,
  AdminRequesterListQuery,
  ChangeRequesterStatusInput,
  CreateAdminRequesterInput,
} from '@/types/adminRequester'

export async function getAdminRequesters(
  query: AdminRequesterListQuery,
): Promise<PageResponse<AdminRequester>> {
  const response = await http.get<ApiResponse<PageResponse<AdminRequester>>>('/admin/requesters', {
    params: { ...query, keyword: query.keyword?.trim() || undefined },
  })
  return response.data.data
}

export async function createAdminRequester(
  input: CreateAdminRequesterInput,
): Promise<AdminRequester> {
  // Explicit allow-list: never send a caller-provided role or skill assignment.
  const payload = {
    account: input.account.trim(),
    initialPassword: input.initialPassword,
    displayName: input.displayName.trim(),
    email: input.email.trim() || null,
    phone: input.phone.trim() || null,
    department: input.department.trim() || null,
    reason: input.reason.trim(),
  }
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminRequester>>('/admin/requesters', payload)
  return response.data.data
}

export async function changeAdminRequesterStatus(
  id: string,
  input: ChangeRequesterStatusInput,
): Promise<AdminRequester> {
  if (!/^[1-9]\d*$/.test(id)) throw new Error('需求方编号格式不正确')
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminRequester>>(`/admin/requesters/${id}/status`, {
    expectedUpdatedAt: input.expectedUpdatedAt,
    status: input.status,
    reason: input.reason.trim(),
  })
  return response.data.data
}
