import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  buildStatisticsCsv,
  downloadStatisticsCsv,
  statisticsCsvFilename,
  type StatisticsExportContext,
} from './statisticsExport'
import type { AdminStatisticsDashboard } from '@/types/statistics'

const dashboard = {
  range: { from: '2026-08-01', to: '2026-08-15', categoryId: null },
  kpis: {
    submittedCount: 4,
    completedCount: 1,
    completionRate: 25,
    firstResponseSampleCount: 2,
    averageFirstResponseHours: 6.5,
  },
  statusDistribution: [{ status: 'COMPLETED', count: 1 }],
  categoryDistribution: [{ categoryId: '3', categoryName: '网站开发', count: 4 }],
  submissionTrend: [{ date: '2026-08-01', count: 4 }],
  memberWorkloads: [
    {
      memberId: '5',
      memberAccount: 'member-a',
      memberName: '成员甲',
      activeCount: 3,
      inProgressCount: 2,
      pendingAcceptanceCount: 1,
    },
  ],
  generatedAt: '2026-08-15T12:00:00Z',
} satisfies AdminStatisticsDashboard

const context: StatisticsExportContext = {
  categoryName: '全部分类',
  statusLabels: {
    DRAFT: '草稿',
    PENDING_REVIEW: '待评估',
    NEED_MORE_INFO: '待补充',
    PENDING_ASSIGNMENT: '待分配',
    IN_PROGRESS: '处理中',
    PENDING_ACCEPTANCE: '待验收',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
    CANCELLED: '已取消',
  },
}

const createObjectUrlDescriptor = Object.getOwnPropertyDescriptor(URL, 'createObjectURL')
const revokeObjectUrlDescriptor = Object.getOwnPropertyDescriptor(URL, 'revokeObjectURL')

afterEach(() => {
  vi.restoreAllMocks()
  if (createObjectUrlDescriptor) {
    Object.defineProperty(URL, 'createObjectURL', createObjectUrlDescriptor)
  } else {
    Reflect.deleteProperty(URL, 'createObjectURL')
  }
  if (revokeObjectUrlDescriptor) {
    Object.defineProperty(URL, 'revokeObjectURL', revokeObjectUrlDescriptor)
  } else {
    Reflect.deleteProperty(URL, 'revokeObjectURL')
  }
})

describe('statistics export', () => {
  it('生成带 UTF-8 BOM 的分区 CSV，并保留当前统计口径', () => {
    const csv = buildStatisticsCsv(dashboard, context)

    expect(csv.startsWith('\uFEFF')).toBe(true)
    expect(csv).toContain('"统计范围","2026-08-01 至 2026-08-15"')
    expect(csv).toContain('"完成率","25.00%"')
    expect(csv).toContain('"已完成","1"')
    expect(csv).toContain('"2026-08-01","4"')
    expect(csv).toContain('"成员负载"')
    expect(csv).toContain('"成员甲","member-a","3","2","1"')
    expect(statisticsCsvFilename(dashboard.range)).toBe('需求统计_2026-08-01_2026-08-15.csv')
  })

  it('转义逗号与引号，并阻止表格软件把文本解释为公式', () => {
    const unsafeDashboard: AdminStatisticsDashboard = {
      ...dashboard,
      categoryDistribution: [
        { categoryId: '9', categoryName: '=HYPERLINK("https://example.test","打开")', count: 1 },
      ],
      memberWorkloads: [
        {
          ...dashboard.memberWorkloads[0]!,
          memberAccount: '@unsafe',
          memberName: '-SUM(1,1)',
        },
      ],
    }

    const csv = buildStatisticsCsv(unsafeDashboard, {
      ...context,
      categoryName: '+SUM(1,1)',
    })

    expect(csv).toContain('"\'+SUM(1,1)"')
    expect(csv).toContain('"\'=HYPERLINK(""https://example.test"",""打开"")"')
    expect(csv).toContain('"\'-SUM(1,1)","\'@unsafe"')
  })

  it('触发隐藏下载链接并释放临时对象地址', () => {
    const createObjectUrl = vi.fn(() => 'blob:statistics')
    const revokeObjectUrl = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createObjectUrl })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeObjectUrl })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)

    expect(downloadStatisticsCsv(dashboard, context)).toBe('需求统计_2026-08-01_2026-08-15.csv')
    expect(createObjectUrl).toHaveBeenCalledOnce()
    expect(click).toHaveBeenCalledOnce()
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:statistics')
    expect(document.querySelector('a[download]')).toBeNull()
  })
})
