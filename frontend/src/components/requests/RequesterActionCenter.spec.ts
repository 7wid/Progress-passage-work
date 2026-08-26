import { RouterLinkStub, shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RequesterActionCenter from './RequesterActionCenter.vue'
import type { RequestStatus, RequestSummary } from '@/types/request'

function request(id: string, status: RequestStatus, title: string): RequestSummary {
  return {
    id,
    requestNo: status === 'DRAFT' ? null : `REQ-${id}`,
    title,
    categoryId: '1',
    categoryName: '网站与系统建设',
    creatorName: '需求方',
    urgency: 'NORMAL',
    status,
    progress: 0,
    expectedDeadline: null,
    submittedAt: status === 'DRAFT' ? null : '2026-08-26T08:00:00Z',
    createdAt: '2026-08-26T08:00:00Z',
  }
}

describe('RequesterActionCenter', () => {
  it('为三类需求方待办提供明确原因和深链接操作', () => {
    const wrapper = shallowMount(RequesterActionCenter, {
      props: {
        total: 3,
        items: [
          request('3', 'PENDING_ACCEPTANCE', '确认报名系统成果'),
          request('2', 'NEED_MORE_INFO', '补充活动流程'),
          request('1', 'DRAFT', '完善设备报修需求'),
        ],
      },
      global: { stubs: { RouterLink: RouterLinkStub, 'el-skeleton': true } },
    })

    expect(wrapper.text()).toContain('优先完成这些事项')
    expect(wrapper.text()).toContain('检查交付内容')
    expect(wrapper.text()).toContain('查看评估意见')
    expect(wrapper.text()).toContain('技术组才会开始评估')

    const destinations = wrapper.findAllComponents(RouterLinkStub).map((link) => link.props('to'))
    expect(destinations).toContainEqual({
      name: 'request-detail',
      params: { id: '3' },
      hash: '#delivery-acceptance',
    })
    expect(destinations).toContainEqual({ name: 'request-edit', params: { id: '2' } })
    expect(destinations).toContainEqual({ name: 'request-edit', params: { id: '1' } })
  })

  it('没有待办时提供说明和发起需求入口', () => {
    const wrapper = shallowMount(RequesterActionCenter, {
      props: { total: 0, items: [] },
      global: { stubs: { RouterLink: RouterLinkStub, 'el-skeleton': true } },
    })

    expect(wrapper.text()).toContain('目前没有需要你操作的事项')
    expect(wrapper.findComponent(RouterLinkStub).props('to')).toBe('/requests/new')
  })

  it('待办超过展示上限时保留查看全部入口', () => {
    const wrapper = shallowMount(RequesterActionCenter, {
      props: {
        total: 8,
        items: Array.from({ length: 6 }, (_, index) =>
          request(String(index + 1), 'DRAFT', `草稿 ${index + 1}`),
        ),
      },
      global: { stubs: { RouterLink: RouterLinkStub, 'el-skeleton': true } },
    })

    expect(wrapper.findAll('.action-item')).toHaveLength(5)
    expect(wrapper.text()).toContain('还有 3 项待办')
  })
})
