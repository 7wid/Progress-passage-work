import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAdminStatistics } from './statistics'
import { http } from './http'
import type { AdminStatisticsDashboard } from '@/types/statistics'

vi.mock('./http', () => ({ http: { get: vi.fn() } }))

const getMock = vi.mocked(http.get)
const dashboard = {
  range: { from: '2026-08-01', to: '2026-08-15', categoryId: null },
  kpis: {
    submittedCount: 4,
    completedCount: 1,
    completionRate: 25,
    firstResponseSampleCount: 2,
    averageFirstResponseHours: 6.5,
  },
  statusDistribution: [],
  categoryDistribution: [],
  submissionTrend: [],
  generatedAt: '2026-08-15T12:00:00Z',
} satisfies AdminStatisticsDashboard

describe('statistics api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('发送统一日期范围、保留大整数分类 ID 并解包响应', async () => {
    getMock.mockResolvedValueOnce({ data: { data: dashboard, requestId: 'r1' } } as never)
    await expect(
      getAdminStatistics({
        from: '2026-08-01',
        to: '2026-08-15',
        categoryId: '9007199254740993',
      }),
    ).resolves.toEqual(dashboard)
    expect(getMock).toHaveBeenCalledWith('/admin/statistics', {
      params: {
        from: '2026-08-01',
        to: '2026-08-15',
        categoryId: '9007199254740993',
      },
    })
  })

  it('在发起请求前拒绝非法范围和非法分类 ID', async () => {
    await expect(getAdminStatistics({ from: '2026-08-15', to: '2026-08-01' })).rejects.toThrow(
      '开始日期不能晚于结束日期',
    )
    await expect(
      getAdminStatistics({ from: '2026-08-01', to: '2026-08-15', categoryId: '1.5' }),
    ).rejects.toThrow('分类编号格式不正确')
    await expect(getAdminStatistics({ from: '2026-02-31', to: '2026-08-15' })).rejects.toThrow(
      '开始日期格式不正确',
    )
    expect(getMock).not.toHaveBeenCalled()
  })
})
