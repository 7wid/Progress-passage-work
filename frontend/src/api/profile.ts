import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  ChangePasswordInput,
  PasswordChangeResult,
  UpdateProfileInput,
  UserProfile,
} from '@/types/profile'

export async function getProfile(): Promise<UserProfile> {
  const response = await http.get<ApiResponse<UserProfile>>('/users/me')
  return response.data.data
}

export async function updateProfile(input: UpdateProfileInput): Promise<UserProfile> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.patch<ApiResponse<UserProfile>>('/users/me', {
    displayName: input.displayName.trim(),
    email: input.email.trim() || null,
    phone: input.phone.trim() || null,
    department: input.department.trim() || null,
  })
  return response.data.data
}

export async function changePassword(input: ChangePasswordInput): Promise<PasswordChangeResult> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.put<ApiResponse<PasswordChangeResult>>('/users/me/password', input)
  return response.data.data
}
