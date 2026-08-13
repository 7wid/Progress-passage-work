import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  deletePendingAttachment,
  getRequestAttachments,
  uploadRequestAttachment,
  validateAttachmentFile,
} from './attachments'
import { http } from './http'
import type { AttachmentRecord, AttachmentSnapshot } from '@/types/attachment'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)
const deleteMock = vi.mocked(http.delete)

describe('attachments api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('按业务类型读取附件快照', async () => {
    const snapshot = { requestId: '10' } as AttachmentSnapshot
    getMock.mockResolvedValueOnce({ data: { data: snapshot } } as never)

    await expect(getRequestAttachments('10', 'REQUEST')).resolves.toEqual(snapshot)
    expect(getMock).toHaveBeenCalledWith('/requests/10/attachments', {
      params: { businessType: 'REQUEST', pendingOnly: false },
    })
  })

  it('读取刷新后仍未绑定的交付附件', async () => {
    const snapshot = { requestId: '10' } as AttachmentSnapshot
    getMock.mockResolvedValueOnce({ data: { data: snapshot } } as never)

    await expect(getRequestAttachments('10', 'DELIVERY', true)).resolves.toEqual(snapshot)
    expect(getMock).toHaveBeenCalledWith('/requests/10/attachments', {
      params: { businessType: 'DELIVERY', pendingOnly: true },
    })
  })

  it('非法编号不会发送请求', async () => {
    await expect(getRequestAttachments('../10', 'REQUEST')).rejects.toThrow('需求编号格式不正确')
    await expect(deletePendingAttachment('10', '../20')).rejects.toThrow('附件编号格式不正确')
    expect(getMock).not.toHaveBeenCalled()
    expect(deleteMock).not.toHaveBeenCalled()
  })

  it('获取 CSRF 后上传 FormData 并回报进度', async () => {
    const record = { id: '20' } as AttachmentRecord
    getMock.mockResolvedValueOnce({ data: { data: 'token' } } as never)
    postMock.mockImplementationOnce(async (_path, body, config) => {
      expect(body).toBeInstanceOf(FormData)
      expect((body as FormData).get('businessType')).toBe('DELIVERY')
      expect((body as FormData).get('file')).toBeInstanceOf(File)
      config?.onUploadProgress?.({ loaded: 5, total: 10 } as never)
      return { data: { data: record } } as never
    })
    const progress = vi.fn()

    await expect(
      uploadRequestAttachment(
        '10',
        'DELIVERY',
        new File(['hello'], 'manual.txt', { type: 'text/plain' }),
        progress,
      ),
    ).resolves.toBe(record)
    expect(progress).toHaveBeenCalledWith(50)
    expect(postMock.mock.calls[0]?.[2]).not.toHaveProperty('headers.Content-Type')
  })

  it('在网络请求前拒绝超限和危险文件', async () => {
    const oversized = new File(['x'], 'large.pdf', { type: 'application/pdf' })
    Object.defineProperty(oversized, 'size', { value: 20 * 1024 * 1024 + 1 })

    expect(validateAttachmentFile(oversized)).toContain('20 MB')
    expect(
      validateAttachmentFile(new File(['x'], 'payload.exe', { type: 'application/octet-stream' })),
    ).toContain('不支持')
    await expect(uploadRequestAttachment('10', 'REQUEST', oversized)).rejects.toThrow('20 MB')
    expect(getMock).not.toHaveBeenCalled()
  })

  it('删除待绑定附件前先获取 CSRF', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'token' } } as never)
    deleteMock.mockResolvedValueOnce({} as never)
    await deletePendingAttachment('10', '20')
    expect(deleteMock).toHaveBeenCalledWith('/requests/10/attachments/20')
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(
      deleteMock.mock.invocationCallOrder[0]!,
    )
  })
})
