import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { ElMessageBox } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AttachmentUploader from './AttachmentUploader.vue'
import { deletePendingAttachment, uploadRequestAttachment } from '@/api/attachments'
import type { AttachmentRecord } from '@/types/attachment'

vi.mock('@/api/attachments', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/attachments')>()),
  uploadRequestAttachment: vi.fn(),
  deletePendingAttachment: vi.fn(),
}))

const uploadMock = vi.mocked(uploadRequestAttachment)
const deleteMock = vi.mocked(deletePendingAttachment)
const ButtonStub = defineComponent({
  setup(_props, { slots }) {
    return () => h('button', slots.default?.())
  },
})

function mountUploader(modelValue: AttachmentRecord[] = []) {
  return shallowMount(AttachmentUploader, {
    props: {
      requestId: '10',
      businessType: 'DELIVERY',
      modelValue,
    },
    global: {
      stubs: {
        AttachmentList: true,
        'el-button': ButtonStub,
        'el-progress': true,
      },
    },
  })
}

describe('AttachmentUploader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(undefined as never)
  })

  it('串行上传选择的文件并发布服务端附件', async () => {
    let active = 0
    let maxActive = 0
    uploadMock.mockImplementation(async (_id, _type, file) => {
      active += 1
      maxActive = Math.max(maxActive, active)
      await Promise.resolve()
      active -= 1
      return {
        id: file.name === 'a.txt' ? '21' : '22',
        requestId: '10',
        businessType: 'DELIVERY',
        businessId: null,
        originalName: file.name,
        contentType: 'text/plain',
        sizeBytes: file.size,
        uploaderId: '2',
        uploaderName: '负责人',
        canDelete: true,
        createdAt: '2026-08-13T08:00:00Z',
      }
    })
    const wrapper = mountUploader()
    const input = wrapper.get('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [
        new File(['a'], 'a.txt', { type: 'text/plain' }),
        new File(['b'], 'b.txt', { type: 'text/plain' }),
      ],
    })
    await input.trigger('change')
    await flushPromises()

    expect(uploadMock).toHaveBeenCalledTimes(2)
    expect(maxActive).toBe(1)
    const updates = wrapper.emitted('update:modelValue') ?? []
    expect(updates[updates.length - 1]?.[0]).toHaveLength(2)
    expect(wrapper.emitted('uploading-change')).toEqual([[true], [false]])
  })

  it('阻止同名同大小的重复附件', async () => {
    const existing = {
      id: '21',
      requestId: '10',
      businessType: 'DELIVERY',
      businessId: null,
      originalName: 'a.txt',
      contentType: 'text/plain',
      sizeBytes: 1,
      uploaderId: '2',
      uploaderName: '负责人',
      canDelete: true,
      createdAt: '2026-08-13T08:00:00Z',
    } satisfies AttachmentRecord
    const wrapper = mountUploader([existing])
    const input = wrapper.get('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['a'], 'a.txt', { type: 'text/plain' })],
    })
    await input.trigger('change')
    await flushPromises()
    expect(uploadMock).not.toHaveBeenCalled()
  })

  it('优先显示后端返回的安全业务错误消息', async () => {
    uploadMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          error: {
            message: '文件内容与扩展名不匹配',
          },
        },
      },
    })
    const wrapper = mountUploader()
    const input = wrapper.get('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['not-pdf'], 'report.pdf', { type: 'application/pdf' })],
    })

    await input.trigger('change')
    await flushPromises()

    expect(wrapper.text()).toContain('文件内容与扩展名不匹配')
    expect(wrapper.text()).not.toContain('Request failed with status code')
  })

  it('使用适用于需求附件和交付附件的通用删除确认文案', async () => {
    const existing = {
      id: '21',
      requestId: '10',
      businessType: 'REQUEST',
      businessId: '10',
      originalName: 'requirement.pdf',
      contentType: 'application/pdf',
      sizeBytes: 1024,
      uploaderId: '2',
      uploaderName: '需求方',
      canDelete: true,
      createdAt: '2026-08-13T08:00:00Z',
    } satisfies AttachmentRecord
    deleteMock.mockResolvedValue(undefined)
    const wrapper = mountUploader([existing])

    wrapper.findComponent({ name: 'AttachmentList' }).vm.$emit('remove', existing)
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确定删除附件“requirement.pdf”吗？',
      '删除附件',
      expect.any(Object),
    )
    expect(deleteMock).toHaveBeenCalledWith('10', '21')
  })
})
