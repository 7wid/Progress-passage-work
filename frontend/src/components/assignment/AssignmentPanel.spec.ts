import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AssignmentPanel from './AssignmentPanel.vue'
import { getRequestMemberRecommendations } from '@/api/assignments'
import type { MemberRecommendationResult, RequestAssignment } from '@/types/assignment'

vi.mock('@/api/assignments', () => ({
  getRequestMemberRecommendations: vi.fn(),
  updateRequestAssignment: vi.fn(),
}))

const getRecommendationsMock = vi.mocked(getRequestMemberRecommendations)

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

const recommendationResult: MemberRecommendationResult = {
  requiredSkills: 'Java、数据库',
  members: [
    {
      id: '4',
      account: 'member04',
      displayName: '成员推荐甲',
      role: 'MEMBER',
      skills: ['Java', '数据库'],
      matchedSkills: ['Java', '数据库'],
      activeRequestCount: 1,
      projectedActiveRequestCount: 2,
      rank: 1,
    },
  ],
}

describe('AssignmentPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getRecommendationsMock.mockResolvedValue({ requiredSkills: null, members: [] })
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
    expect(getRecommendationsMock).not.toHaveBeenCalled()
  })

  it('管理员在可分配状态加载候选成员并显示编辑表单', async () => {
    const wrapper = shallowMount(AssignmentPanel, {
      props: { assignment, isAdmin: true },
      global: { stubs },
    })
    await flushPromises()

    expect(getRecommendationsMock).toHaveBeenCalledWith('100')
    expect(wrapper.text()).toContain('保存任务成员')
  })

  it('展示可解释的前三名推荐并允许带入负责人选择', async () => {
    getRecommendationsMock.mockResolvedValue(recommendationResult)
    const wrapper = shallowMount(AssignmentPanel, {
      props: { assignment, isAdmin: true },
      global: { stubs },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('成员推荐甲')
    expect(wrapper.text()).toContain('匹配技能：Java、数据库')
    expect(wrapper.text()).toContain('分配后预计')

    const selectButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('选为负责人'))
    if (!selectButton) throw new Error('未找到推荐选择按钮')
    await selectButton.trigger('click')

    expect(selectButton.attributes('aria-pressed')).toBe('true')
    expect(wrapper.text()).toContain('当前已选择')
  })
})
