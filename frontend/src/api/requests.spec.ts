import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  cancelRequest,
  createDraft,
  getRequestDetail,
  getRequests,
  submitRequest,
  updateRequest,
} from './requests'
import { http } from './http'
import type { RequestDetail } from '@/types/request'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)
const putMock = vi.mocked(http.put)

const requestInput = {
  categoryId: '',
  title: ' 草稿标题 ',
  background: '',
  description: '',
  expectedResult: '',
  expectedDeadline: '',
  urgency: 'NORMAL' as const,
  budgetAmount: '',
  budgetDescription: '',
  technicalConstraints: '',
  contactInfo: '',
  informationConfirmed: false,
}

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

  it('保存不完整草稿时将空字段规范化为 null', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf' } } as never)
    postMock.mockResolvedValueOnce({
      data: { data: { id: '42', requestNo: null, status: 'DRAFT' } },
    } as never)

    await createDraft(requestInput)

    expect(postMock).toHaveBeenCalledWith('/requests/drafts', {
      categoryId: null,
      title: '草稿标题',
      background: null,
      description: null,
      expectedResult: null,
      expectedDeadline: null,
      urgency: 'NORMAL',
      budgetAmount: null,
      budgetDescription: null,
      technicalConstraints: null,
      contactInfo: null,
    })
  })

  it('编辑、提交和取消均传递服务端版本', async () => {
    getMock.mockResolvedValue({ data: { data: 'csrf' } } as never)
    putMock.mockResolvedValueOnce({
      data: { data: { id: '42', requestNo: null, status: 'DRAFT', version: 3 } },
    } as never)
    postMock
      .mockResolvedValueOnce({
        data: {
          data: { id: '42', requestNo: 'REQ-1', status: 'PENDING_REVIEW', version: 4 },
        },
      } as never)
      .mockResolvedValueOnce({
        data: { data: { id: '42', requestNo: 'REQ-1', status: 'CANCELLED', version: 5 } },
      } as never)

    await updateRequest('42', requestInput, 2)
    await submitRequest('42', 3)
    await cancelRequest('42', 4, ' 计划已经取消 ')

    expect(putMock).toHaveBeenCalledWith(
      '/requests/42',
      expect.objectContaining({ expectedVersion: 2 }),
    )
    expect(postMock).toHaveBeenNthCalledWith(1, '/requests/42/submit', {
      expectedVersion: 3,
      informationConfirmed: true,
    })
    expect(postMock).toHaveBeenNthCalledWith(2, '/requests/42/cancel', {
      expectedVersion: 4,
      reason: '计划已经取消',
    })
  })
})
