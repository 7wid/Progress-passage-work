import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import StatisticsView from './StatisticsView.vue'
import { getAdminCategories } from '@/api/adminCategories'
import { getAdminStatistics } from '@/api/statistics'
import type { AdminStatisticsDashboard } from '@/types/statistics'
import { downloadStatisticsCsv } from '@/utils/statisticsExport'

vi.mock('@/api/adminCategories', () => ({ getAdminCategories: vi.fn() }))
vi.mock('@/api/statistics', () => ({ getAdminStatistics: vi.fn() }))
vi.mock('@/utils/statisticsExport', () => ({ downloadStatisticsCsv: vi.fn() }))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}))

const categoriesMock = vi.mocked(getAdminCategories)
const statisticsMock = vi.mocked(getAdminStatistics)
const downloadMock = vi.mocked(downloadStatisticsCsv)
const messageSuccessMock = vi.mocked(ElMessage.success)
const messageErrorMock = vi.mocked(ElMessage.error)
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

const ButtonStub = defineComponent({
  props: { disabled: Boolean, loading: Boolean },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h(
        'button',
        { disabled: props.disabled || props.loading, onClick: () => emit('click') },
        slots.default?.(),
      )
  },
})

function buttonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const button = wrapper.findAll('button').find((item) => item.text().includes(text))
  if (!button) throw new Error(`未找到按钮：${text}`)
  return button
}

function mountView() {
  return shallowMount(StatisticsView, {
    global: {
      directives: { loading: () => undefined },
      stubs: {
        AppPageHeader: false,
        'el-alert': { template: '<aside><slot /></aside>' },
        'el-button': ButtonStub,
        'el-card': { template: '<section><slot name="header"/><slot /></section>' },
        'el-date-picker': true,
        'el-empty': true,
        'el-option': true,
        'el-select': true,
        'el-skeleton': true,
        MemberWorkloadTable: { template: '<div>成员负载明细</div>' },
      },
    },
  })
}

describe('StatisticsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    categoriesMock.mockResolvedValue([])
    statisticsMock.mockResolvedValue(dashboard)
    downloadMock.mockReturnValue('需求统计_2026-08-01_2026-08-15.csv')
  })

  it('进入页面时并行加载分类与本月统计并解释统计口径', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(categoriesMock).toHaveBeenCalledTimes(1)
    expect(statisticsMock).toHaveBeenCalledTimes(1)
    expect(statisticsMock.mock.calls[0]?.[0].from).toMatch(/^\d{4}-\d{2}-01$/)
    expect(wrapper.text()).toContain('新增需求')
    expect(wrapper.text()).toContain('平均首次响应')
    expect(wrapper.text()).toContain('第一条评估距发起时间')
    expect(wrapper.text()).toContain('成员负载')
  })

  it('统计加载失败时保留重试入口', async () => {
    statisticsMock.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('重新加载')
    expect(wrapper.text()).not.toContain('新增需求')
    expect(buttonByText(wrapper, '导出 CSV').attributes('disabled')).toBeDefined()
  })

  it('导出当前已加载的统计结果并反馈文件名', async () => {
    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, '导出 CSV').trigger('click')

    expect(downloadMock).toHaveBeenCalledWith(
      dashboard,
      expect.objectContaining({ categoryName: '全部分类' }),
    )
    expect(messageSuccessMock).toHaveBeenCalledWith('已导出 需求统计_2026-08-01_2026-08-15.csv')
  })

  it('浏览器拒绝下载时给出可恢复的错误反馈', async () => {
    downloadMock.mockImplementationOnce(() => {
      throw new Error('blocked')
    })
    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, '导出 CSV').trigger('click')

    expect(messageErrorMock).toHaveBeenCalledWith('报表导出失败，请重试')
  })
})
