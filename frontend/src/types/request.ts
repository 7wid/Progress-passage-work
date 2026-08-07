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

export interface RequestSummary {
  id: string
  requestNo: string
  title: string
  categoryName: string
  status: RequestStatus
  progress: number
  createdAt: string
}
