import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  ConfirmRejectionResult,
  CreatedEvaluationResult,
  CreateEvaluationInput,
  EvaluationRecord,
} from '@/types/evaluation'

function requestPath(requestId: string): string {
  return `/requests/${encodeURIComponent(requestId)}/evaluations`
}

function trimToNull(value: string | null): string | null {
  if (value === null) return null

  const trimmed = value.trim()
  return trimmed.length === 0 ? null : trimmed
}

export async function getEvaluations(requestId: string): Promise<EvaluationRecord[]> {
  const response = await http.get<ApiResponse<EvaluationRecord[]>>(requestPath(requestId))

  return response.data.data
}

export async function createEvaluation(
  requestId: string,
  input: CreateEvaluationInput,
): Promise<CreatedEvaluationResult> {
  await http.get<ApiResponse<string>>('/auth/csrf')

  const feasible = input.conclusion === 'FEASIBLE'

  const response = await http.post<ApiResponse<CreatedEvaluationResult>>(requestPath(requestId), {
    ...input,
    publicComment: input.publicComment.trim(),
    solutionSummary: feasible ? trimToNull(input.solutionSummary) : null,
    estimatedWorkload: feasible ? input.estimatedWorkload : null,
    estimatedFinishAt: feasible ? input.estimatedFinishAt : null,
    requiredSkills: trimToNull(input.requiredSkills),
    risks: trimToNull(input.risks),
    internalNote: trimToNull(input.internalNote),
  })

  return response.data.data
}

export async function confirmEvaluationRejection(
  requestId: string,
  evaluationId: string,
  requestVersion: number,
): Promise<ConfirmRejectionResult> {
  await http.get<ApiResponse<string>>('/auth/csrf')

  const response = await http.post<ApiResponse<ConfirmRejectionResult>>(
    `${requestPath(requestId)}/${encodeURIComponent(evaluationId)}/confirm-rejection`,
    { requestVersion },
  )

  return response.data.data
}
