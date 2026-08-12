import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { ElMessageBox } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProgressPanel from './ProgressPanel.vue'
import { createProgress } from '@/api/progress'
import type { CreatedProgressResult, RequestProgressSnapshot } from '@/types/progress'

vi.mock('@/api/progress', () => ({
  createProgress: vi.fn(),
}))

const createProgressMock = vi.mocked(createProgress)

const FormStub = defineComponent({
  emits: ['submit'],
  setup(_props, { emit, expose, slots }) {
    expose({
      validate: async () => true,
      clearValidate: () => undefined,
    })

    return () =>
      h(
        'form',
        {
          onSubmit: (event: Event) => {
            event.preventDefault()
            emit('submit', event)
          },
        },
        slots.default?.(),
      )
  },
})

const stubs = {
  'el-card': { template: '<section><slot /></section>' },
  'el-progress': true,
  'el-divider': true,
  'el-form': FormStub,
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input-number': true,
  'el-input': true,
  'el-date-picker': true,
  'el-switch': true,
  'el-alert': true,
  'el-button': { template: '<button><slot /></button>' },
  'el-empty': true,
  'el-timeline': { template: '<div><slot /></div>' },
  'el-timeline-item': { template: '<div><slot /></div>' },
  'el-tag': { template: '<span><slot /></span>' },
}

function snapshot(canUpdateProgress: boolean): RequestProgressSnapshot {
  return {
    requestId: '100',
    requestStatus: 'IN_PROGRESS',
    requestVersion: 3,
    currentProgress: 30,
    lastProgressAt: '2026-08-11T08:00:00Z',
    nextUpdateAt: null,
    needsFollowUp: false,
    canUpdateProgress,
    logs: [
      {
        id: '1',
        requestId: '100',
        authorId: '2',
        authorName: '负责人甲',
        progress: 30,
        content: '完成后端接口开发',
        nextPlan: null,
        nextUpdateAt: null,
        visibleToRequester: false,
        createdAt: '2026-08-11T08:00:00Z',
      },
    ],
  }
}

describe('ProgressPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(undefined as never)
  })

  it('无更新权限时只读展示当前进度', () => {
    const wrapper = shallowMount(ProgressPanel, {
      props: { snapshot: snapshot(false) },
      global: { stubs },
    })

    expect(wrapper.text()).toContain('当前进度')
    expect(wrapper.text()).not.toContain('发布进度')
  })

  it('成功发布后抛出 updated 事件', async () => {
    const result = {
      currentProgress: 30,
      requestVersion: 4,
    } as CreatedProgressResult
    createProgressMock.mockResolvedValue(result)

    const wrapper = shallowMount(ProgressPanel, {
      props: { snapshot: snapshot(true) },
      global: { stubs },
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createProgressMock).toHaveBeenCalledWith(
      '100',
      expect.objectContaining({ requestVersion: 3, progress: 30 }),
    )
    expect(wrapper.emitted('updated')).toEqual([[result]])
  })

  it('并发冲突时抛出 conflict 事件', async () => {
    createProgressMock.mockRejectedValue({
      isAxiosError: true,
      response: { status: 409 },
    })

    const wrapper = shallowMount(ProgressPanel, {
      props: { snapshot: snapshot(true) },
      global: { stubs },
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.emitted('conflict')).toHaveLength(1)
  })
})
