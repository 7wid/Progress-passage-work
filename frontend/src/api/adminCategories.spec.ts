import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  changeAdminCategoryStatus,
  createAdminCategory,
  getAdminCategories,
  updateAdminCategory,
} from './adminCategories'
import { http } from './http'
import type { AdminCategory } from '@/types/admin'

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

const category = {
  id: '6',
  name: '网站开发',
  sortOrder: 10,
  enabled: true,
  createdAt: '2026-08-14T08:00:00Z',
  updatedAt: '2026-08-14T08:00:00Z',
} satisfies AdminCategory

describe('admin categories api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('读取包含停用项的管理分类列表', async () => {
    getMock.mockResolvedValueOnce({ data: { data: [category] } } as never)
    await expect(getAdminCategories()).resolves.toEqual([category])
    expect(getMock).toHaveBeenCalledWith('/admin/categories')
  })

  it('获取 CSRF 后创建或更新分类并清理文本', async () => {
    getMock.mockResolvedValue({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: category } } as never)
    putMock.mockResolvedValueOnce({ data: { data: category } } as never)

    await createAdminCategory({ name: '  网站开发  ', sortOrder: 10, reason: '  新增业务分类  ' })
    await updateAdminCategory('6', {
      expectedUpdatedAt: category.updatedAt,
      name: '  网站开发  ',
      sortOrder: 20,
      reason: '  调整分类展示顺序  ',
    })

    expect(postMock).toHaveBeenCalledWith('/admin/categories', {
      name: '网站开发',
      sortOrder: 10,
      reason: '新增业务分类',
    })
    expect(putMock).toHaveBeenCalledWith('/admin/categories/6', {
      expectedUpdatedAt: category.updatedAt,
      name: '网站开发',
      sortOrder: 20,
      reason: '调整分类展示顺序',
    })
  })

  it('获取 CSRF 后启停分类', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: category } } as never)

    await changeAdminCategoryStatus('6', {
      expectedUpdatedAt: category.updatedAt,
      enabled: false,
      reason: '  暂停接收此类需求  ',
    })

    expect(postMock).toHaveBeenCalledWith('/admin/categories/6/status', {
      expectedUpdatedAt: category.updatedAt,
      enabled: false,
      reason: '暂停接收此类需求',
    })
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })

  it('路径中的长十进制 ID 保持原始字符串精度', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: category } } as never)

    await changeAdminCategoryStatus('9007199254740993', {
      expectedUpdatedAt: category.updatedAt,
      enabled: false,
      reason: '暂停接收此类需求',
    })

    expect(postMock).toHaveBeenCalledWith('/admin/categories/9007199254740993/status', {
      expectedUpdatedAt: category.updatedAt,
      enabled: false,
      reason: '暂停接收此类需求',
    })
  })

  it('非法分类 ID 不会发送请求', async () => {
    await expect(
      updateAdminCategory('../6', {
        expectedUpdatedAt: category.updatedAt,
        name: '网站开发',
        sortOrder: 10,
        reason: '调整分类显示名称',
      }),
    ).rejects.toThrow('分类编号格式不正确')
    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
    expect(putMock).not.toHaveBeenCalled()
  })
})
