import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AttachmentUploader from './AttachmentUploader.vue'
import { uploadRequestAttachment } from '@/api/attachments'
import type { AttachmentRecord } from '@/types/attachment'

vi.mock('@/api/attachments', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/attachments')>()),
  uploadRequestAttachment: vi.fn(),
  deletePendingAttachment: vi.fn(),
}))

const uploadMock = vi.mocked(uploadRequestAttachment)
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
  beforeEach(() => vi.clearAllMocks())

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
})
