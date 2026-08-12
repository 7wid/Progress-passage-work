import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RequestTimeline from './RequestTimeline.vue'
import type { ProgressLog } from '@/types/progress'
import type { RequestStatusHistory } from '@/types/request'

describe('RequestTimeline', () => {
  it('合并状态变化与进度记录并按时间倒序展示', () => {
    const statusHistory: RequestStatusHistory[] = [
      {
        id: '1',
        fromStatus: 'PENDING_ASSIGNMENT',
        toStatus: 'IN_PROGRESS',
        reason: '管理员完成任务分配',
        operatorName: '管理员',
        createdAt: '2026-08-10T08:00:00Z',
      },
    ]
    const progressLogs: ProgressLog[] = [
      {
        id: '2',
        requestId: '100',
        authorId: '2',
        authorName: '负责人',
        progress: 40,
        content: '完成接口开发',
        nextPlan: null,
        nextUpdateAt: null,
        visibleToRequester: false,
        createdAt: '2026-08-11T08:00:00Z',
      },
    ]

    const wrapper = shallowMount(RequestTimeline, {
      props: { statusHistory, progressLogs },
      global: {
        stubs: {
          'el-card': { template: '<section><slot /></section>' },
          'el-empty': true,
          'el-timeline': { template: '<div><slot /></div>' },
          'el-timeline-item': { template: '<div><slot /></div>' },
          'el-tag': { template: '<span><slot /></span>' },
          'el-progress': true,
          RequestStatusTag: true,
        },
      },
    })

    const text = wrapper.text()
    expect(text).toContain('完成接口开发')
    expect(text).toContain('仅技术组可见')
    expect(text).toContain('管理员完成任务分配')
    expect(text.indexOf('完成接口开发')).toBeLessThan(text.indexOf('管理员完成任务分配'))
  })
})
