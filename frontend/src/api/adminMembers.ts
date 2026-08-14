import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  AdminMember,
  AdminMemberListQuery,
  ChangeAdminMemberStatusInput,
  CreateAdminMemberInput,
  SkillTag,
  UpdateAdminMemberInput,
} from '@/types/admin'

function requirePositiveId(value: string, label: string): string {
  if (!/^[1-9]\d*$/.test(value)) throw new Error(`${label}格式不正确`)
  return value
}

function toNumericIds(values: string[], label: string): number[] {
  const ids = values.map((value) => Number(requirePositiveId(value, label)))
  if (ids.some((id) => !Number.isSafeInteger(id))) throw new Error(`${label}格式不正确`)
  if (new Set(ids).size !== ids.length) throw new Error(`${label}不能重复`)
  return ids
}

function nullableText(value: string): string | null {
  return value.trim() || null
}

function memberPath(id: string): string {
  return `/admin/members/${encodeURIComponent(requirePositiveId(id, '成员编号'))}`
}

export async function getAdminMembers(
  query: AdminMemberListQuery,
): Promise<PageResponse<AdminMember>> {
  const response = await http.get<ApiResponse<PageResponse<AdminMember>>>('/admin/members', {
    params: {
      ...query,
      keyword: query.keyword?.trim() || undefined,
    },
  })
  return response.data.data
}

export async function getAdminMember(id: string): Promise<AdminMember> {
  const response = await http.get<ApiResponse<AdminMember>>(memberPath(id))
  return response.data.data
}

export async function getAdminSkillTags(): Promise<SkillTag[]> {
  const response = await http.get<ApiResponse<SkillTag[]>>('/admin/skill-tags')
  return response.data.data
}

export async function createAdminMember(input: CreateAdminMemberInput): Promise<AdminMember> {
  const payload = {
    account: input.account.trim(),
    initialPassword: input.initialPassword,
    displayName: input.displayName.trim(),
    email: nullableText(input.email),
    phone: nullableText(input.phone),
    department: nullableText(input.department),
    role: input.role,
    skillIds: toNumericIds(input.skillIds, '技能编号'),
    reason: input.reason.trim(),
  }
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminMember>>('/admin/members', payload)
  return response.data.data
}

export async function updateAdminMember(
  id: string,
  input: UpdateAdminMemberInput,
): Promise<AdminMember> {
  const payload = {
    expectedUpdatedAt: input.expectedUpdatedAt,
    displayName: input.displayName.trim(),
    email: nullableText(input.email),
    phone: nullableText(input.phone),
    department: nullableText(input.department),
    role: input.role,
    skillIds: toNumericIds(input.skillIds, '技能编号'),
    reason: input.reason.trim(),
  }
  const path = memberPath(id)
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.put<ApiResponse<AdminMember>>(path, payload)
  return response.data.data
}

export async function changeAdminMemberStatus(
  id: string,
  input: ChangeAdminMemberStatusInput,
): Promise<AdminMember> {
  const path = `${memberPath(id)}/status`
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AdminMember>>(path, {
    expectedUpdatedAt: input.expectedUpdatedAt,
    status: input.status,
    reason: input.reason.trim(),
  })
  return response.data.data
}
