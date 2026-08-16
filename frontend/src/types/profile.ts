import type { UserRole } from './auth'

export interface UserProfile {
  id: string
  account: string
  displayName: string
  email: string | null
  phone: string | null
  department: string | null
  role: UserRole
}

export interface UpdateProfileInput {
  displayName: string
  email: string
  phone: string
  department: string
}

export interface ChangePasswordInput {
  currentPassword: string
  newPassword: string
}

export interface PasswordChangeResult {
  otherSessionsInvalidated: boolean
}
