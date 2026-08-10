import type { RequestStatus } from '@/types/request'

export type EvaluationConclusion = 'FEASIBLE' | 'NEED_MORE_INFO' | 'NOT_FEASIBLE'
export type WorkloadUnit = 'HOURS'

export interface EvaluationRecord {
  id: string
  requestId: string
  evaluatorId: string
  evaluatorName: string
  conclusion: EvaluationConclusion
  publicComment: string
  solutionSummary: string | null
  estimatedWorkload: number | null
  workloadUnit: WorkloadUnit | null
  estimatedFinishAt: string | null
  requiredSkills: string | null
  risks: string | null
  internalNote: string | null
  version: number
  createdAt: string
}

export interface CreateEvaluationInput {
  requestVersion: number
  conclusion: EvaluationConclusion
  publicComment: string
  solutionSummary: string | null
  estimatedWorkload: number | null
  estimatedFinishAt: string | null
  requiredSkills: string | null
  risks: string | null
  internalNote: string | null
}

export interface CreatedEvaluationResult {
  evaluation: EvaluationRecord
  requestStatus: RequestStatus
  requestVersion: number
  adminConfirmationRequired: boolean
}

export interface ConfirmRejectionResult {
  requestId: string
  requestStatus: 'REJECTED'
  requestVersion: number
}
