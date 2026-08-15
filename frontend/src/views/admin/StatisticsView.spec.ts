import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StatisticsView from './StatisticsView.vue'
import { getAdminCategories } from '@/api/adminCategories'
import { getAdminStatistics } from '@/api/statistics'
import type { AdminStatisticsDashboard } from '@/types/statistics'

vi.mock('@/api/adminCategories', () => ({ getAdminCategories: vi.fn() }))
vi.mock('@/api/statistics', () => ({ getAdminStatistics: vi.fn() }))

const categoriesMock = vi.mocked(getAdminCategories)
const statisticsMock = vi.mocked(getAdminStatistics)
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
  generatedAt: '2026-08-15T12:00:00Z',
} satisfies AdminStatisticsDashboard

const ButtonStub = defineComponent({
  props: { loading: Boolean },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h('button', { disabled: props.loading, onClick: () => emit('click') }, slots.default?.())
  },
})

function mountView() {
  return shallowMount(StatisticsView, {
    global: {
      directives: { loading: () => undefined },
      stubs: {
        'el-alert': { template: '<aside><slot /></aside>' },
        'el-button': ButtonStub,
        'el-card': { template: '<section><slot name="header"/><slot /></section>' },
        'el-date-picker': true,
        'el-empty': true,
        'el-option': true,
        'el-select': true,
        'el-skeleton': true,
      },
    },
  })
}

describe('StatisticsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    categoriesMock.mockResolvedValue([])
    statisticsMock.mockResolvedValue(dashboard)
  })

  it('进入页面时并行加载分类与本月统计并解释统计口径', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(categoriesMock).toHaveBeenCalledTimes(1)
    expect(statisticsMock).toHaveBeenCalledTimes(1)
    expect(statisticsMock.mock.calls[0]?.[0].from).toMatch(/^\d{4}-\d{2}-01$/)
    expect(wrapper.text()).toContain('新增需求')
    expect(wrapper.text()).toContain('平均首次响应')
    expect(wrapper.text()).toContain('第一条评估距提交时间')
  })

  it('统计加载失败时保留重试入口', async () => {
    statisticsMock.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('重新加载')
    expect(wrapper.text()).not.toContain('新增需求')
  })
})
