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
