import { getApiStatus, http } from './http'
import type { ApiResponse } from '@/types/api'
import type { CurrentUser, LoginInput, RegisterInput, RegistrationStatus } from '@/types/auth'
import type { UserProfile } from '@/types/profile'

async function withFreshCsrf<T>(request: () => Promise<T>): Promise<T> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  try {
    return await request()
  } catch (error) {
    if (getApiStatus(error) !== 403) throw error

    await http.get<ApiResponse<string>>('/auth/csrf')
    return request()
  }
}

export async function login(input: LoginInput): Promise<CurrentUser> {
  const response = await withFreshCsrf(() =>
    http.post<ApiResponse<CurrentUser>>('/auth/login', input),
  )
  return response.data.data
}

export async function logout(): Promise<void> {
  await withFreshCsrf(() => http.post('/auth/logout'))
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
  const response = await withFreshCsrf(() =>
    http.post<ApiResponse<UserProfile>>('/users/register', {
      account: input.account.trim(),
      password: input.password,
      displayName: input.displayName.trim(),
      email: input.email.trim(),
      phone: input.phone.trim() || null,
      department: input.department.trim() || null,
    }),
  )
  return response.data.data
}
