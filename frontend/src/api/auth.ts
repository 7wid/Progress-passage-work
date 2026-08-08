import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { CurrentUser, LoginInput } from '@/types/auth'

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
