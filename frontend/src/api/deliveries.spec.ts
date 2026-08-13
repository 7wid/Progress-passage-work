import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAcceptance, createDelivery, getDeliveryAcceptance } from './deliveries'
import { http } from './http'
import type {
  CreatedAcceptanceResult,
  CreatedDeliveryResult,
  DeliveryAcceptanceSnapshot,
} from '@/types/delivery'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

describe('delivery and acceptance api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('读取交付与验收快照', async () => {
    const snapshot = { requestId: '10' } as DeliveryAcceptanceSnapshot
    getMock.mockResolvedValueOnce({ data: { data: snapshot } } as never)

    await expect(getDeliveryAcceptance('10')).resolves.toEqual(snapshot)
    expect(getMock).toHaveBeenCalledWith('/requests/10/delivery-acceptance')
  })

  it('非法需求编号不会发送 HTTP 请求', async () => {
    await expect(getDeliveryAcceptance('../10')).rejects.toThrow('需求编号格式不正确')
    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
  })

  it('获取 CSRF 后提交经过清理的交付信息', async () => {
    const result = { requestVersion: 5 } as CreatedDeliveryResult
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await expect(
      createDelivery('10', {
        requestVersion: 4,
        description: '  已部署测试环境并整理使用说明  ',
        deliveryUrl: '  https://example.com/releases/1  ',
      }),
    ).resolves.toEqual(result)

    expect(postMock).toHaveBeenCalledWith('/requests/10/deliveries', {
      requestVersion: 4,
      description: '已部署测试环境并整理使用说明',
      deliveryUrl: 'https://example.com/releases/1',
    })
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })

  it('在获取 CSRF 前拒绝非 http/https 交付地址', async () => {
    await expect(
      createDelivery('10', {
        requestVersion: 4,
        description: '已完成交付',
        deliveryUrl: 'javascript:alert(1)',
      }),
    ).rejects.toThrow('完整 http 或 https 链接')

    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
  })

  it('在获取 CSRF 前拒绝含账号密码的交付地址', async () => {
    await expect(
      createDelivery('10', {
        requestVersion: 4,
        description: '已完成交付',
        deliveryUrl: 'https://user:secret@example.com/delivery',
      }),
    ).rejects.toThrow('不含账号密码')

    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
  })

  it('验收请求携带版本、结论和清理后的评价', async () => {
    const result = { requestVersion: 6 } as CreatedAcceptanceResult
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await expect(
      createAcceptance('10', {
        requestVersion: 5,
        result: 'REWORK_REQUIRED',
        comment: '  请补充部署文档和回滚步骤  ',
      }),
    ).resolves.toEqual(result)

    expect(postMock).toHaveBeenCalledWith('/requests/10/acceptance', {
      requestVersion: 5,
      result: 'REWORK_REQUIRED',
      comment: '请补充部署文档和回滚步骤',
    })
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })
})
