import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  CreateAcceptanceInput,
  CreateDeliveryInput,
  CreatedAcceptanceResult,
  CreatedDeliveryResult,
  DeliveryAcceptanceSnapshot,
} from '@/types/delivery'

function requestBasePath(requestId: string): string {
  if (!/^[1-9]\d*$/.test(requestId)) {
    throw new Error('需求编号格式不正确')
  }

  return `/requests/${encodeURIComponent(requestId)}`
}

function trimToNull(value: string | null): string | null {
  if (value === null) return null

  const trimmed = value.trim()
  return trimmed.length === 0 ? null : trimmed
}

function normalizeDeliveryUrl(value: string | null): string | null {
  const trimmed = trimToNull(value)
  if (trimmed === null) return null

  let url: URL
  try {
    url = new URL(trimmed)
  } catch {
    throw new Error('交付地址必须是完整的 http 或 https 链接')
  }

  const allowedProtocol = url.protocol === 'http:' || url.protocol === 'https:'
  const safeAuthority = Boolean(url.hostname) && !url.username && !url.password
  if (!allowedProtocol || !safeAuthority) {
    throw new Error('交付地址必须是不含账号密码的完整 http 或 https 链接')
  }

  return trimmed
}

export async function getDeliveryAcceptance(
  requestId: string,
): Promise<DeliveryAcceptanceSnapshot> {
  const response = await http.get<ApiResponse<DeliveryAcceptanceSnapshot>>(
    `${requestBasePath(requestId)}/delivery-acceptance`,
  )

  return response.data.data
}

export async function createDelivery(
  requestId: string,
  input: CreateDeliveryInput,
): Promise<CreatedDeliveryResult> {
  const path = `${requestBasePath(requestId)}/deliveries`
  const payload = {
    requestVersion: input.requestVersion,
    description: input.description.trim(),
    deliveryUrl: normalizeDeliveryUrl(input.deliveryUrl),
  }

  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<CreatedDeliveryResult>>(path, payload)

  return response.data.data
}

export async function createAcceptance(
  requestId: string,
  input: CreateAcceptanceInput,
): Promise<CreatedAcceptanceResult> {
  const path = `${requestBasePath(requestId)}/acceptance`
  const payload = {
    requestVersion: input.requestVersion,
    result: input.result,
    comment: trimToNull(input.comment),
  }

  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<CreatedAcceptanceResult>>(path, payload)

  return response.data.data
}
