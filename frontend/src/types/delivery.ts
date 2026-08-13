import type { RequestStatus } from '@/types/request'

export type AcceptanceResult = 'ACCEPTED' | 'REWORK_REQUIRED'

export interface DeliveryRecord {
  id: string
  requestId: string
  submitterId: string
  submitterName: string
  description: string
  deliveryUrl: string | null
  createdAt: string
}

export interface AcceptanceRecord {
  id: string
  requestId: string
  deliveryId: string | null
  operatorId: string
  operatorName: string
  result: AcceptanceResult
  comment: string | null
  createdAt: string
}

export interface DeliveryAcceptanceSnapshot {
  requestId: string
  requestStatus: RequestStatus
  requestVersion: number
  canSubmitDelivery: boolean
  canAccept: boolean
  deliveries: DeliveryRecord[]
  acceptances: AcceptanceRecord[]
}

export interface CreateDeliveryInput {
  requestVersion: number
  description: string
  deliveryUrl: string | null
}

export interface CreatedDeliveryResult {
  delivery: DeliveryRecord
  requestStatus: RequestStatus
  requestVersion: number
}

export interface CreateAcceptanceInput {
  requestVersion: number
  result: AcceptanceResult
  comment: string | null
}

export interface CreatedAcceptanceResult {
  acceptance: AcceptanceRecord
  requestStatus: RequestStatus
  requestVersion: number
}
