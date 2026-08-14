import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { AdminRequestActionInput, AdminRequestActionResult } from '@/types/admin'

function requestActionPath(id: string, action: 'cancel' | 'reopen'): string {
  if (!/^[1-9]\d*$/.test(id)) throw new Error('需求编号格式不正确')
  return `/admin/requests/${encodeURIComponent(id)}/${action}`
}

async function submitAdminRequestAction(
  id: string,
  action: 'cancel' | 'reopen',
  input: AdminRequestActionInput,
): Promise<AdminRequestActionResult> {
  const path = requestActionPath(id, action)
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminRequestActionResult>>(path, {
    expectedVersion: input.expectedVersion,
    reason: input.reason.trim(),
  })
  return response.data.data
}

export function cancelRequestAsAdmin(
  id: string,
  input: AdminRequestActionInput,
): Promise<AdminRequestActionResult> {
  return submitAdminRequestAction(id, 'cancel', input)
}

export function reopenRequestAsAdmin(
  id: string,
  input: AdminRequestActionInput,
): Promise<AdminRequestActionResult> {
  return submitAdminRequestAction(id, 'reopen', input)
}
