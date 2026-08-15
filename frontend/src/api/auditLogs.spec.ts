import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAdminAuditLogs } from './auditLogs'
import { http } from './http'
import type { AuditLogPage } from '@/types/audit'

vi.mock('./http', () => ({ http: { get: vi.fn() } }))

const getMock = vi.mocked(http.get)

describe('audit logs api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('清理筛选条件、保留大整数 ID 并解包分页', async () => {
    const page = {
      items: [],
      page: 1,
      pageSize: 20,
      total: 0,
      totalPages: 0,
    } satisfies AuditLogPage
    getMock.mockResolvedValueOnce({ data: { data: page, requestId: 'r1' } } as never)

    await expect(
      getAdminAuditLogs({
        page: 1,
        pageSize: 20,
        actorId: '9007199254740993',
        action: '  MEMBER_UPDATE  ',
        targetType: ' USER ',
        targetId: ' 12 ',
        requestId: ' trace-1 ',
        from: '2026-08-01',
        to: '2026-08-15',
      }),
    ).resolves.toEqual(page)

    expect(getMock).toHaveBeenCalledWith('/admin/audit-logs', {
      params: {
        page: 1,
        pageSize: 20,
        actorId: '9007199254740993',
        action: 'MEMBER_UPDATE',
        targetType: 'USER',
        targetId: '12',
        requestId: 'trace-1',
        from: '2026-08-01',
        to: '2026-08-15',
      },
    })
  })

  it('在 HTTP 前拒绝非法 ID、日期与分页', async () => {
    await expect(getAdminAuditLogs({ page: 0, pageSize: 20 })).rejects.toThrow('页码格式不正确')
    await expect(getAdminAuditLogs({ page: 1, pageSize: 20, actorId: '1.5' })).rejects.toThrow(
      '操作者编号格式不正确',
    )
    await expect(getAdminAuditLogs({ page: 1, pageSize: 20, from: '2026-02-31' })).rejects.toThrow(
      '开始日期格式不正确',
    )
    await expect(
      getAdminAuditLogs({ page: 1, pageSize: 20, from: '2026-08-15', to: '2026-08-01' }),
    ).rejects.toThrow('开始日期不能晚于结束日期')
    expect(getMock).not.toHaveBeenCalled()
  })
})
