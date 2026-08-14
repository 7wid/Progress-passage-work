import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  AdminCategory,
  ChangeAdminCategoryStatusInput,
  CreateAdminCategoryInput,
  UpdateAdminCategoryInput,
} from '@/types/admin'

function requirePositiveId(value: string): string {
  if (!/^[1-9]\d*$/.test(value)) throw new Error('分类编号格式不正确')
  return value
}

function categoryPath(id: string): string {
  return `/admin/categories/${encodeURIComponent(requirePositiveId(id))}`
}

export async function getAdminCategories(): Promise<AdminCategory[]> {
  const response = await http.get<ApiResponse<AdminCategory[]>>('/admin/categories')
  return response.data.data
}

export async function createAdminCategory(input: CreateAdminCategoryInput): Promise<AdminCategory> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminCategory>>('/admin/categories', {
    name: input.name.trim(),
    sortOrder: input.sortOrder,
    reason: input.reason.trim(),
  })
  return response.data.data
}

export async function updateAdminCategory(
  id: string,
  input: UpdateAdminCategoryInput,
): Promise<AdminCategory> {
  const path = categoryPath(id)
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.put<ApiResponse<AdminCategory>>(path, {
    expectedUpdatedAt: input.expectedUpdatedAt,
    name: input.name.trim(),
    sortOrder: input.sortOrder,
    reason: input.reason.trim(),
  })
  return response.data.data
}

export async function changeAdminCategoryStatus(
  id: string,
  input: ChangeAdminCategoryStatusInput,
): Promise<AdminCategory> {
  const path = `${categoryPath(id)}/status`
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminCategory>>(path, {
    expectedUpdatedAt: input.expectedUpdatedAt,
    enabled: input.enabled,
    reason: input.reason.trim(),
  })
  return response.data.data
}
