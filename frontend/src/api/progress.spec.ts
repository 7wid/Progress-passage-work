import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createProgress, getRequestProgress } from './progress'
import { http } from './http'
import type { CreatedProgressResult, RequestProgressSnapshot } from '@/types/progress'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

describe('progress api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('使用合法需求 ID 并解包进度快照', async () => {
    const snapshot = { requestId: '10' } as RequestProgressSnapshot
    getMock.mockResolvedValueOnce({ data: { data: snapshot } } as never)

    await expect(getRequestProgress('10')).resolves.toEqual(snapshot)
    expect(getMock).toHaveBeenCalledWith('/requests/10/progress')
  })

  it('非法需求 ID 不发送 HTTP 请求', async () => {
    await expect(getRequestProgress('10/20')).rejects.toThrow('需求编号格式不正确')
    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
  })

  it('创建前获取 CSRF 并清理文本字段', async () => {
    const result = { currentProgress: 35, requestVersion: 4 } as CreatedProgressResult
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await expect(
      createProgress('10', {
        requestVersion: 3,
        progress: 35,
        content: '  已完成登录接口联调  ',
        nextPlan: '   ',
        nextUpdateAt: '2030-09-01T08:00:00.000Z',
        visibleToRequester: true,
      }),
    ).resolves.toEqual(result)

    expect(postMock).toHaveBeenCalledWith('/requests/10/progress', {
      requestVersion: 3,
      progress: 35,
      content: '已完成登录接口联调',
      nextPlan: null,
      nextUpdateAt: '2030-09-01T08:00:00.000Z',
      visibleToRequester: true,
    })

    const csrfOrder = getMock.mock.invocationCallOrder[0]
    const postOrder = postMock.mock.invocationCallOrder[0]
    if (csrfOrder === undefined || postOrder === undefined) {
      throw new Error('测试没有捕获到预期的 HTTP 调用')
    }
    expect(csrfOrder).toBeLessThan(postOrder)
  })
})
