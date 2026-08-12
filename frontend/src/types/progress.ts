import type { RequestStatus } from '@/types/request'

export interface ProgressLog {
  id: string
  requestId: string
  authorId: string
  authorName: string
  progress: number
  content: string
  nextPlan: string | null
  nextUpdateAt: string | null
  visibleToRequester: boolean
  createdAt: string
}

export interface RequestProgressSnapshot {
  requestId: string
  requestStatus: RequestStatus
  requestVersion: number
  currentProgress: number
  lastProgressAt: string | null
  nextUpdateAt: string | null
  needsFollowUp: boolean
  canUpdateProgress: boolean
  logs: ProgressLog[]
}

export interface CreateProgressInput {
  requestVersion: number
  progress: number
  content: string
  nextPlan: string | null
  nextUpdateAt: string | null
  visibleToRequester: boolean
}

export interface CreatedProgressResult {
  log: ProgressLog
  currentProgress: number
  requestVersion: number
}
