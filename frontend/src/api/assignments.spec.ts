import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getAssignableMemberOptions,
  getRequestAssignment,
  updateRequestAssignment,
} from './assignments'
import { http } from './http'
import type { RequestAssignment } from '@/types/assignment'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    put: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const putMock = vi.mocked(http.put)

describe('assignments api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('清理候选成员关键词并解包响应', async () => {
    const options = [
      { id: '2', account: 'member01', displayName: '成员一', role: 'MEMBER' as const },
    ]
    getMock.mockResolvedValueOnce({ data: { data: options } } as never)

    await expect(getAssignableMemberOptions('  member  ')).resolves.toEqual(options)
    expect(getMock).toHaveBeenCalledWith('/members/options', {
      params: { keyword: 'member' },
    })
  })

  it('编码需求 ID 并解包成员配置', async () => {
    const assignment = { requestId: '10' } as RequestAssignment
    getMock.mockResolvedValueOnce({ data: { data: assignment } } as never)

    await expect(getRequestAssignment('10/20')).resolves.toEqual(assignment)
    expect(getMock).toHaveBeenCalledWith('/requests/10%2F20/members')
  })

  it('更新前获取 CSRF 并发送数字成员 ID', async () => {
    const assignment = { requestId: '10' } as RequestAssignment
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    putMock.mockResolvedValueOnce({ data: { data: assignment } } as never)

    await updateRequestAssignment('10', {
      requestVersion: 2,
      ownerId: '8',
      participantIds: ['9', '10'],
      reason: '  根据技术方向调整负责人  ',
    })

    expect(putMock).toHaveBeenCalledWith('/requests/10/members', {
      requestVersion: 2,
      ownerId: 8,
      participantIds: [9, 10],
      reason: '根据技术方向调整负责人',
    })

    const csrfOrder = getMock.mock.invocationCallOrder[0]
    const putOrder = putMock.mock.invocationCallOrder[0]
    if (csrfOrder === undefined || putOrder === undefined) {
      throw new Error('测试没有捕获到预期的 HTTP 调用')
    }
    expect(csrfOrder).toBeLessThan(putOrder)
  })

  it('成员 ID 非法时不发送 HTTP 请求', async () => {
    await expect(
      updateRequestAssignment('10', {
        requestVersion: 2,
        ownerId: 'not-a-number',
        participantIds: [],
        reason: '有效的任务分配原因',
      }),
    ).rejects.toThrow('成员编号格式不正确')

    expect(getMock).not.toHaveBeenCalled()
    expect(putMock).not.toHaveBeenCalled()
  })
})
