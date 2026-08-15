import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { AuditLogPage, AuditLogQuery } from '@/types/audit'

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function optionalText(value: string | undefined): string | undefined {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

function optionalPositiveId(value: string | undefined): string | undefined {
  const trimmed = optionalText(value)
  if (trimmed === undefined) return undefined
  if (!/^[1-9]\d*$/.test(trimmed)) throw new Error('操作者编号格式不正确')
  return trimmed
}

function optionalDate(value: string | undefined, label: string): string | undefined {
  if (value === undefined || value === '') return undefined
  if (!ISO_DATE_PATTERN.test(value)) throw new Error(`${label}格式不正确`)
  const year = Number(value.slice(0, 4))
  const month = Number(value.slice(5, 7))
  const day = Number(value.slice(8, 10))
  const date = new Date(Date.UTC(year, month - 1, day))
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    throw new Error(`${label}格式不正确`)
  }
  return value
}

export async function getAdminAuditLogs(query: AuditLogQuery): Promise<AuditLogPage> {
  if (!Number.isInteger(query.page) || query.page < 1) throw new Error('页码格式不正确')
  if (!Number.isInteger(query.pageSize) || query.pageSize < 1 || query.pageSize > 100) {
    throw new Error('每页数量格式不正确')
  }
  const from = optionalDate(query.from, '开始日期')
  const to = optionalDate(query.to, '结束日期')
  if (from && to && from > to) throw new Error('开始日期不能晚于结束日期')

  const response = await http.get<ApiResponse<AuditLogPage>>('/admin/audit-logs', {
    params: {
      page: query.page,
      pageSize: query.pageSize,
      actorId: optionalPositiveId(query.actorId),
      action: optionalText(query.action),
      targetType: optionalText(query.targetType),
      targetId: optionalText(query.targetId),
      requestId: optionalText(query.requestId),
      from,
      to,
    },
  })
  return response.data.data
}
