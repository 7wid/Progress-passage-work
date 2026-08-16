export type UserRole = 'REQUESTER' | 'MEMBER' | 'ADMIN'

export interface CurrentUser {
  id: string
  account: string
  displayName: string
  role: UserRole
}

export interface LoginInput {
  account: string
  password: string
}

export interface RegistrationStatus {
  enabled: boolean
  emailSuffix: string | null
}

export interface RegisterInput {
  account: string
  password: string
  displayName: string
  email: string
  phone: string
  department: string
}
