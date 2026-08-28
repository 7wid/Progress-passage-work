import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RequestSupplementGuide from './RequestSupplementGuide.vue'
import { getEvaluations } from '@/api/evaluations'
import type { EvaluationRecord } from '@/types/evaluation'

vi.mock('@/api/evaluations', () => ({ getEvaluations: vi.fn() }))

function evaluation(overrides: Partial<EvaluationRecord> = {}): EvaluationRecord {
  return {
    id: '10',
    requestId: '101',
    evaluatorId: '2',
    evaluatorName: '评估成员',
    conclusion: 'NEED_MORE_INFO',
    publicComment: '请补充使用人数和数据来源。',
    solutionSummary: null,
    estimatedWorkload: null,
    workloadUnit: null,
    estimatedFinishAt: null,
    requiredSkills: null,
    risks: null,
    internalNote: '不应展示的内部备注',
    version: 1,
    createdAt: '2026-08-28T08:00:00Z',
    ...overrides,
  }
}

function mountGuide() {
  return shallowMount(RequestSupplementGuide, {
    props: { requestId: '101' },
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="`/requests/${to.params.id}`"><slot /></a>',
        },
        'el-button': { template: '<button><slot /></button>' },
      },
    },
  })
}

describe('补充资料指引', () => {
  beforeEach(() => vi.resetAllMocks())

  it('按评估版本展示最新公开说明，不渲染内部信息或 HTML', async () => {
    const comment = '补充说明第一行\n<img src=x onerror=alert(1)>第二行'
    vi.mocked(getEvaluations).mockResolvedValue([
      evaluation(),
      evaluation({ id: '11', version: 2, publicComment: comment }),
    ])
    const wrapper = mountGuide()
    expect(wrapper.text()).toContain('正在加载补充要求')
    await flushPromises()
    expect(getEvaluations).toHaveBeenCalledWith('101')
    expect(wrapper.text()).toContain(comment)
    expect(wrapper.text()).toContain('第 2 次评估')
    expect(wrapper.text()).not.toContain('不应展示的内部备注')
    expect(wrapper.text()).not.toContain('请补充使用人数')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.get('a').attributes('href')).toBe('/requests/101')
    expect(wrapper.text()).toContain('保存不会重新进入评估')
    wrapper.unmount()
  })

  it.each([
    { records: [] },
    { records: [evaluation({ conclusion: 'FEASIBLE', version: 2 }), evaluation()] },
  ])('没有最新补充结论时不误展示历史要求', async ({ records }) => {
    vi.mocked(getEvaluations).mockResolvedValue(records)
    const wrapper = mountGuide()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无可展示的最新补充要求')
    expect(wrapper.text()).not.toContain('请补充使用人数')
    wrapper.unmount()
  })

  it('加载失败有可访问提示并可重试恢复', async () => {
    vi.mocked(getEvaluations)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce([evaluation()])
    const wrapper = mountGuide()
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('补充要求加载失败')
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('请补充使用人数和数据来源')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('切换需求后忽略迟到的旧响应', async () => {
    let resolveOld!: (records: EvaluationRecord[]) => void
    vi.mocked(getEvaluations)
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveOld = resolve
        }),
      )
      .mockResolvedValueOnce([
        evaluation({ requestId: '202', publicComment: '另一个需求的补充要求' }),
      ])
    const wrapper = mountGuide()
    await wrapper.setProps({ requestId: '202' })
    await flushPromises()
    resolveOld([evaluation()])
    await flushPromises()
    expect(wrapper.text()).toContain('另一个需求的补充要求')
    expect(wrapper.text()).not.toContain('请补充使用人数')
    expect(wrapper.get('a').attributes('href')).toBe('/requests/202')
    wrapper.unmount()
  })
})
