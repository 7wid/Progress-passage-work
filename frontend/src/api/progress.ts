import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  CreatedProgressResult,
  CreateProgressInput,
  RequestProgressSnapshot,
} from '@/types/progress'

function progressPath(requestId: string): string {
  if (!/^[1-9]\d*$/.test(requestId)) {
    throw new Error('需求编号格式不正确')
  }
  return `/requests/${encodeURIComponent(requestId)}/progress`
}

function trimToNull(value: string | null): string | null {
  if (value === null) return null

  const trimmed = value.trim()
  return trimmed.length === 0 ? null : trimmed
}

export async function getRequestProgress(requestId: string): Promise<RequestProgressSnapshot> {
  const response = await http.get<ApiResponse<RequestProgressSnapshot>>(progressPath(requestId))

  return response.data.data
}

export async function createProgress(
  requestId: string,
  input: CreateProgressInput,
): Promise<CreatedProgressResult> {
  const payload = {
    requestVersion: input.requestVersion,
    progress: input.progress,
    content: input.content.trim(),
    nextPlan: trimToNull(input.nextPlan),
    nextUpdateAt: input.nextUpdateAt,
    visibleToRequester: input.visibleToRequester,
  }

  await http.get<ApiResponse<string>>('/auth/csrf')

  const response = await http.post<ApiResponse<CreatedProgressResult>>(
    progressPath(requestId),
    payload,
  )

  return response.data.data
}
