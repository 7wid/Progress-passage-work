import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cancelRequestAsAdmin, reopenRequestAsAdmin } from './adminRequests'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

describe('admin requests api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('获取 CSRF 后取消需求并清理原因', async () => {
    const result = { id: '10', status: 'CANCELLED', version: 4 } as const
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await expect(
      cancelRequestAsAdmin('10', { expectedVersion: 3, reason: '  线下确认需求终止  ' }),
    ).resolves.toEqual(result)

    expect(postMock).toHaveBeenCalledWith('/admin/requests/10/cancel', {
      expectedVersion: 3,
      reason: '线下确认需求终止',
    })
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })

  it('获取 CSRF 后重新开启需求', async () => {
    const result = { id: '10', status: 'IN_PROGRESS', version: 5 } as const
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await expect(
      reopenRequestAsAdmin('10', { expectedVersion: 4, reason: '确认继续处理该需求' }),
    ).resolves.toEqual(result)
    expect(postMock).toHaveBeenCalledWith('/admin/requests/10/reopen', {
      expectedVersion: 4,
      reason: '确认继续处理该需求',
    })
  })

  it('路径中的长十进制 ID 保持原始字符串精度', async () => {
    const result = { id: '9007199254740993', status: 'IN_PROGRESS', version: 5 } as const
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await reopenRequestAsAdmin('9007199254740993', {
      expectedVersion: 4,
      reason: '确认继续处理该需求',
    })

    expect(postMock).toHaveBeenCalledWith('/admin/requests/9007199254740993/reopen', {
      expectedVersion: 4,
      reason: '确认继续处理该需求',
    })
  })

  it('非法需求编号在 CSRF 前被拒绝', async () => {
    await expect(
      cancelRequestAsAdmin('../10', { expectedVersion: 3, reason: '确认终止该需求' }),
    ).rejects.toThrow('需求编号格式不正确')
    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
  })
})
