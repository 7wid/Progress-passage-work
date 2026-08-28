export type RequesterAccountStatus = 'ACTIVE' | 'DISABLED'

export interface AdminRequester {
  id: string
  account: string
  displayName: string
  email: string | null
  phone: string | null
  department: string | null
  status: RequesterAccountStatus
  createdAt: string
  updatedAt: string
}

export interface AdminRequesterListQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: RequesterAccountStatus
}

export interface CreateAdminRequesterInput {
  account: string
  initialPassword: string
  displayName: string
  email: string
  phone: string
  department: string
  reason: string
}

export interface ChangeRequesterStatusInput {
  expectedUpdatedAt: string
  status: RequesterAccountStatus
  reason: string
}
