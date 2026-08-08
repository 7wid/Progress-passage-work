export type RequestStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'NEED_MORE_INFO'
  | 'PENDING_ASSIGNMENT'
  | 'IN_PROGRESS'
  | 'PENDING_ACCEPTANCE'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED'

export type RequestUrgency = 'NORMAL' | 'HIGH' | 'URGENT'

export interface RequestSummary {
  id: string
  requestNo: string
  title: string
  categoryName: string
  status: RequestStatus
  progress: number
  createdAt: string
}
export interface CategoryOption {
  id: string
  name: string
}

export interface CreateRequestInput {
  categoryId: string
  title: string
  background: string
  description: string
  expectedResult: string
  expectedDeadline: string
  urgency: RequestUrgency
  budgetAmount: string
  budgetDescription: string
  technicalConstraints: string
  contactInfo: string
  informationConfirmed: boolean
}

export interface CreatedRequest {
  id: string
  requestNo: string
  status: 'PENDING_REVIEW'
}
