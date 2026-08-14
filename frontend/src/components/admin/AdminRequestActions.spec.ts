import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminRequestActions from './AdminRequestActions.vue'
import AdminReasonDialog from './AdminReasonDialog.vue'
import { cancelRequestAsAdmin, reopenRequestAsAdmin } from '@/api/adminRequests'

vi.mock('@/api/adminRequests', () => ({
  cancelRequestAsAdmin: vi.fn(),
  reopenRequestAsAdmin: vi.fn(),
}))

const cancelMock = vi.mocked(cancelRequestAsAdmin)
const reopenMock = vi.mocked(reopenRequestAsAdmin)

const ButtonStub = defineComponent({
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () => h('button', { onClick: () => emit('click') }, slots.default?.())
  },
})

function mountActions(status: 'IN_PROGRESS' | 'COMPLETED') {
  return shallowMount(AdminRequestActions, {
    props: { requestId: '10', status, version: 3 },
    global: {
      stubs: {
        'el-card': { template: '<section><slot /></section>' },
        'el-alert': true,
        'el-button': ButtonStub,
      },
    },
  })
}

describe('AdminRequestActions', () => {
  beforeEach(() => vi.clearAllMocks())

  it('处理中只显示取消操作并提交当前服务端版本', async () => {
    const result = { id: '10', status: 'CANCELLED', version: 4 } as const
    cancelMock.mockResolvedValue(result)
    const wrapper = mountActions('IN_PROGRESS')

    expect(wrapper.text()).toContain('管理员取消需求')
    expect(wrapper.text()).not.toContain('重新开启需求')
    await wrapper.get('button').trigger('click')
    wrapper.findComponent(AdminReasonDialog).vm.$emit('confirm', '线下确认需求终止')
    await flushPromises()

    expect(cancelMock).toHaveBeenCalledWith('10', {
      expectedVersion: 3,
      reason: '线下确认需求终止',
    })
    expect(wrapper.emitted('updated')).toEqual([[result]])
  })

  it('已完成需求只显示重新开启操作', async () => {
    const result = { id: '10', status: 'IN_PROGRESS', version: 4 } as const
    reopenMock.mockResolvedValue(result)
    const wrapper = mountActions('COMPLETED')

    expect(wrapper.text()).toContain('重新开启需求')
    expect(wrapper.text()).not.toContain('管理员取消需求')
    await wrapper.get('button').trigger('click')
    wrapper.findComponent(AdminReasonDialog).vm.$emit('confirm', '需求方确认继续处理')
    await flushPromises()

    expect(reopenMock).toHaveBeenCalledWith('10', {
      expectedVersion: 3,
      reason: '需求方确认继续处理',
    })
    expect(wrapper.emitted('updated')).toEqual([[result]])
  })

  it('409 冲突时要求父页面整页刷新且不抛出成功事件', async () => {
    cancelMock.mockRejectedValue({ isAxiosError: true, response: { status: 409 } })
    const wrapper = mountActions('IN_PROGRESS')

    await wrapper.get('button').trigger('click')
    wrapper.findComponent(AdminReasonDialog).vm.$emit('confirm', '线下确认需求终止')
    await flushPromises()

    expect(wrapper.emitted('conflict')).toHaveLength(1)
    expect(wrapper.emitted('updated')).toBeUndefined()
  })
})
