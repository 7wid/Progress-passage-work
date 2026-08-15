import type { PageResponse } from '@/types/api'

export type AuditJson =
  null | string | number | boolean | AuditJson[] | { [key: string]: AuditJson }

export interface AuditLogRecord {
  id: string
  actorId: string | null
  actorName: string
  action: string
  targetType: string
  targetId: string | null
  beforeData: AuditJson
  afterData: AuditJson
  requestId: string | null
  ipAddress: string | null
  createdAt: string
}

export interface AuditLogQuery {
  page: number
  pageSize: number
  actorId?: string
  action?: string
  targetType?: string
  targetId?: string
  requestId?: string
  from?: string
  to?: string
}

export type AuditLogPage = PageResponse<AuditLogRecord>
