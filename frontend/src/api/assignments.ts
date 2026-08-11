import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  AssignableMemberOption,
  RequestAssignment,
  UpdateRequestMembersInput,
} from '@/types/assignment'

function membersPath(requestId: string): string {
  return `/requests/${encodeURIComponent(requestId)}/members`
}

function toNumericId(id: string): number {
  const value = Number(id)
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new Error('成员编号格式不正确')
  }
  return value
}

export async function getAssignableMemberOptions(keyword = ''): Promise<AssignableMemberOption[]> {
  const response = await http.get<ApiResponse<AssignableMemberOption[]>>('/members/options', {
    params: { keyword: keyword.trim() || undefined },
  })
  return response.data.data
}

export async function getRequestAssignment(requestId: string): Promise<RequestAssignment> {
  const response = await http.get<ApiResponse<RequestAssignment>>(membersPath(requestId))
  return response.data.data
}

export async function updateRequestAssignment(
  requestId: string,
  input: UpdateRequestMembersInput,
): Promise<RequestAssignment> {
  const payload = {
    requestVersion: input.requestVersion,
    ownerId: toNumericId(input.ownerId),
    participantIds: input.participantIds.map(toNumericId),
    reason: input.reason.trim(),
  }

  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.put<ApiResponse<RequestAssignment>>(membersPath(requestId), payload)
  return response.data.data
}
