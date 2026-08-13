import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AttachmentList from './AttachmentList.vue'
import type { AttachmentRecord } from '@/types/attachment'

vi.mock('@/api/attachments', () => ({
  downloadAttachment: vi.fn(),
}))

const pendingAttachment: AttachmentRecord = {
  id: '20',
  requestId: '10',
  businessType: 'DELIVERY',
  businessId: null,
  originalName: '<b>manual.pdf</b>',
  contentType: 'application/pdf',
  sizeBytes: 2048,
  uploaderId: '2',
  uploaderName: '负责人',
  canDelete: true,
  createdAt: '2026-08-13T08:00:00Z',
}

describe('AttachmentList', () => {
  it('以纯文本显示文件名且待绑定附件可删除', async () => {
    const wrapper = shallowMount(AttachmentList, {
      props: { attachments: [pendingAttachment] },
      global: {
        stubs: {
          'el-button': { template: '<button @click="$emit(\'click\')"><slot /></button>' },
        },
      },
    })
    expect(wrapper.find('b').exists()).toBe(false)
    expect(wrapper.text()).toContain('<b>manual.pdf</b>')
    await wrapper.findAll('button')[1]!.trigger('click')
    expect(wrapper.emitted('remove')?.[0]).toEqual([pendingAttachment])
  })

  it('已绑定附件不能删除', () => {
    const wrapper = shallowMount(AttachmentList, {
      props: {
        attachments: [{ ...pendingAttachment, businessId: '30', canDelete: false }],
      },
      global: { stubs: { 'el-button': { template: '<button><slot /></button>' } } },
    })
    expect(wrapper.findAll('button')).toHaveLength(1)
  })
})
