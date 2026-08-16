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
  requestNo: string | null
  status: 'DRAFT' | 'PENDING_REVIEW'
}

export interface RequestMutation {
  id: string
  requestNo: string | null
  status: RequestStatus
  version: number
}

export type RequestSort = 'NEWEST' | 'OLDEST' | 'DEADLINE_ASC' | 'DEADLINE_DESC'

export interface RequestListQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: RequestStatus
  categoryId?: string
  submittedFrom?: string
  submittedTo?: string
  sort: RequestSort
  assignmentType?: 'OWNER' | 'PARTICIPANT'
  activeOnly?: boolean
  overdue?: boolean
}

export interface RequestSummary {
  id: string
  requestNo: string | null
  title: string
  categoryId: string | null
  categoryName: string
  creatorName: string
  urgency: RequestUrgency | null
  status: RequestStatus
  progress: number
  expectedDeadline: string | null
  submittedAt: string | null
  createdAt: string
}

export interface RequestStatusHistory {
  id: string
  fromStatus: RequestStatus | null
  toStatus: RequestStatus
  reason: string | null
  operatorName: string
  createdAt: string
}

export interface RequestDetail extends RequestSummary {
  version: number
  creatorId: string
  background: string | null
  description: string | null
  expectedResult: string | null
  budgetAmount: number | null
  budgetDescription: string | null
  technicalConstraints: string | null
  contactInfo: string | null
  updatedAt: string
  statusHistory: RequestStatusHistory[]
}
