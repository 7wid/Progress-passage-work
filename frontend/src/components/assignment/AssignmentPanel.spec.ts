import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AssignmentPanel from './AssignmentPanel.vue'
import { getAssignableMemberOptions } from '@/api/assignments'
import type { RequestAssignment } from '@/types/assignment'

vi.mock('@/api/assignments', () => ({
  getAssignableMemberOptions: vi.fn(),
  updateRequestAssignment: vi.fn(),
}))

const getOptionsMock = vi.mocked(getAssignableMemberOptions)

const assignment: RequestAssignment = {
  requestId: '100',
  requestStatus: 'PENDING_ASSIGNMENT',
  requestVersion: 2,
  owner: {
    id: '11',
    userId: '2',
    displayName: '负责人甲',
    role: 'MEMBER',
    memberType: 'OWNER',
    joinedAt: '2026-08-11T08:00:00Z',
  },
  participants: [
    {
      id: '12',
      userId: '3',
      displayName: '参与人乙',
      role: 'MEMBER',
      memberType: 'PARTICIPANT',
      joinedAt: '2026-08-11T08:00:00Z',
    },
  ],
}

const stubs = {
  'el-card': { template: '<section><slot /></section>' },
  'el-descriptions': { template: '<div><slot /></div>' },
  'el-descriptions-item': { template: '<div><slot /></div>' },
  'el-tag': { template: '<span><slot /></span>' },
  'el-divider': true,
  'el-alert': true,
  'el-form': { template: '<form><slot /></form>' },
  'el-form-item': { template: '<div><slot /></div>' },
  'el-select': { template: '<div><slot /></div>' },
  'el-option': true,
  'el-input': true,
  'el-button': { template: '<button><slot /></button>' },
}

describe('AssignmentPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getOptionsMock.mockResolvedValue([])
  })

  it('非管理员只读展示成员且不请求候选列表', async () => {
    const wrapper = shallowMount(AssignmentPanel, {
      props: { assignment, isAdmin: false },
      global: { stubs },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('负责人甲')
    expect(wrapper.text()).toContain('参与人乙')
    expect(wrapper.text()).not.toContain('保存任务成员')
    expect(getOptionsMock).not.toHaveBeenCalled()
  })

  it('管理员在可分配状态加载候选成员并显示编辑表单', async () => {
    const wrapper = shallowMount(AssignmentPanel, {
      props: { assignment, isAdmin: true },
      global: { stubs },
    })
    await flushPromises()

    expect(getOptionsMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('保存任务成员')
  })
})
