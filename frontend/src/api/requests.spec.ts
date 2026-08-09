import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getRequestDetail, getRequests } from './requests'
import { http } from './http'
import type { RequestDetail } from '@/types/request'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)

describe('requests api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('清理关键词并解包分页响应', async () => {
    const pageResponse = {
      items: [],
      page: 2,
      pageSize: 50,
      total: 0,
      totalPages: 0,
    }

    getMock.mockResolvedValueOnce({
      data: { data: pageResponse, requestId: 'request-id' },
    } as never)

    const result = await getRequests({
      page: 2,
      pageSize: 50,
      keyword: '  关键字  ',
      sort: 'NEWEST',
    })

    expect(result).toEqual(pageResponse)
    expect(getMock).toHaveBeenCalledWith('/requests', {
      params: {
        page: 2,
        pageSize: 50,
        keyword: '关键字',
        sort: 'NEWEST',
        categoryId: undefined,
        submittedFrom: undefined,
        submittedTo: undefined,
      },
    })
  })

  it('对详情 ID 编码并解包响应', async () => {
    const detail = { id: '42' } as RequestDetail
    getMock.mockResolvedValueOnce({
      data: { data: detail, requestId: 'request-id' },
    } as never)

    await expect(getRequestDetail('42')).resolves.toEqual(detail)
    expect(getMock).toHaveBeenCalledWith('/requests/42')
  })
})
