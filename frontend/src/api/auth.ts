import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { CurrentUser, LoginInput, RegisterInput, RegistrationStatus } from '@/types/auth'
import type { UserProfile } from '@/types/profile'

export async function login(input: LoginInput): Promise<CurrentUser> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<CurrentUser>>('/auth/login', input)
  return response.data.data
}

export async function logout(): Promise<void> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  await http.post('/auth/logout')
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await http.get<ApiResponse<CurrentUser>>('/auth/me')
  return response.data.data
}

export async function getRegistrationStatus(): Promise<RegistrationStatus> {
  const response = await http.get<ApiResponse<RegistrationStatus>>('/users/registration')
  return response.data.data
}

export async function register(input: RegisterInput): Promise<UserProfile> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<UserProfile>>('/users/register', {
    account: input.account.trim(),
    password: input.password,
    displayName: input.displayName.trim(),
    email: input.email.trim(),
    phone: input.phone.trim() || null,
    department: input.department.trim() || null,
  })
  return response.data.data
}
