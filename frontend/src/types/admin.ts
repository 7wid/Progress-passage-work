import type { UserRole } from '@/types/auth'
import type { RequestStatus } from '@/types/request'

export type AdminMemberRole = Extract<UserRole, 'MEMBER' | 'ADMIN'>
export type AdminMemberStatus = 'ACTIVE' | 'DISABLED'

export interface SkillTag {
  id: string
  name: string
}

export interface AdminMember {
  id: string
  account: string
  displayName: string
  email: string | null
  phone: string | null
  department: string | null
  role: AdminMemberRole
  status: AdminMemberStatus
  skills: SkillTag[]
  activeOwnerRequestCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminMemberListQuery {
  page: number
  pageSize: number
  keyword?: string
  role?: AdminMemberRole
  status?: AdminMemberStatus
}

export interface CreateAdminMemberInput {
  account: string
  initialPassword: string
  displayName: string
  email: string
  phone: string
  department: string
  role: AdminMemberRole
  skillIds: string[]
  reason: string
}

export interface UpdateAdminMemberInput {
  expectedUpdatedAt: string
  displayName: string
  email: string
  phone: string
  department: string
  role: AdminMemberRole
  skillIds: string[]
  reason: string
}

export interface ChangeAdminMemberStatusInput {
  expectedUpdatedAt: string
  status: AdminMemberStatus
  reason: string
}

export interface AdminMemberEditorValue {
  account: string
  initialPassword: string
  displayName: string
  email: string
  phone: string
  department: string
  role: AdminMemberRole
  skillIds: string[]
  reason: string
}

export interface AdminCategory {
  id: string
  name: string
  sortOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateAdminCategoryInput {
  name: string
  sortOrder: number
  reason: string
}

export interface UpdateAdminCategoryInput extends CreateAdminCategoryInput {
  expectedUpdatedAt: string
}

export interface ChangeAdminCategoryStatusInput {
  expectedUpdatedAt: string
  enabled: boolean
  reason: string
}

export interface AdminCategoryEditorValue {
  name: string
  sortOrder: number
  reason: string
}

export interface AdminRequestActionInput {
  expectedVersion: number
  reason: string
}

export interface AdminRequestActionResult {
  id: string
  status: RequestStatus
  version: number
}
