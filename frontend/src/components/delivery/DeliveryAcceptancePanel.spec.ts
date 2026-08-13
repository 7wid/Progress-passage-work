import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { ElMessageBox } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DeliveryAcceptancePanel from './DeliveryAcceptancePanel.vue'
import { createAcceptance, createDelivery } from '@/api/deliveries'
import type {
  CreatedAcceptanceResult,
  CreatedDeliveryResult,
  DeliveryAcceptanceSnapshot,
} from '@/types/delivery'
import type { AttachmentRecord } from '@/types/attachment'

vi.mock('@/api/deliveries', () => ({
  createDelivery: vi.fn(),
  createAcceptance: vi.fn(),
}))

const createDeliveryMock = vi.mocked(createDelivery)
const createAcceptanceMock = vi.mocked(createAcceptance)

const FormStub = defineComponent({
  emits: ['submit'],
  setup(_props, { emit, expose, slots }) {
    expose({ validate: async () => true, clearValidate: () => undefined })
    return () =>
      h(
        'form',
        {
          onSubmit: (event: Event) => {
            event.preventDefault()
            emit('submit', event)
          },
        },
        slots.default?.(),
      )
  },
})

const stubs = {
  'el-card': { template: '<section><slot /></section>' },
  'el-empty': true,
  'el-divider': true,
  'el-form': FormStub,
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input': true,
  'el-alert': true,
  'el-collapse': { template: '<div><slot /></div>' },
  'el-collapse-item': { template: '<div><slot /></div>' },
  'el-radio-group': { template: '<div><slot /></div>' },
  'el-radio': { template: '<span><slot /></span>' },
  'el-button': { template: '<button><slot /></button>' },
}

function snapshot(overrides: Partial<DeliveryAcceptanceSnapshot> = {}): DeliveryAcceptanceSnapshot {
  return {
    requestId: '100',
    requestStatus: 'IN_PROGRESS',
    requestVersion: 7,
    canSubmitDelivery: false,
    canAccept: false,
    deliveries: [],
    acceptances: [],
    ...overrides,
  }
}

describe('DeliveryAcceptancePanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(undefined as never)
  })

  it('只读用户可以看到最新交付但看不到操作表单', () => {
    const wrapper = shallowMount(DeliveryAcceptancePanel, {
      props: {
        snapshot: snapshot({
          deliveries: [
            {
              id: '11',
              requestId: '100',
              submitterId: '2',
              submitterName: '负责人',
              description: '已经部署并提交使用说明',
              deliveryUrl: 'https://example.com/delivery',
              attachments: [],
              createdAt: '2026-08-12T08:00:00Z',
            },
          ],
        }),
      },
      global: { stubs },
    })

    expect(wrapper.text()).toContain('已经部署并提交使用说明')
    expect(wrapper.findAll('form')).toHaveLength(0)
    expect(wrapper.get('a').attributes('rel')).toBe('noopener noreferrer')
  })

  it('有交付权限时成功提交并抛出 updated', async () => {
    const result = { requestVersion: 8 } as CreatedDeliveryResult
    createDeliveryMock.mockResolvedValue(result)
    const wrapper = shallowMount(DeliveryAcceptancePanel, {
      props: { snapshot: snapshot({ canSubmitDelivery: true }) },
      global: { stubs },
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createDeliveryMock).toHaveBeenCalledWith(
      '100',
      expect.objectContaining({ requestVersion: 7 }),
    )
    expect(wrapper.emitted('updated')).toEqual([[result]])
  })

  it('刷新后恢复待绑定附件并提交其编号', async () => {
    const result = { requestVersion: 8 } as CreatedDeliveryResult
    createDeliveryMock.mockResolvedValue(result)
    const pendingAttachment = {
      id: '31',
      requestId: '100',
      businessType: 'DELIVERY',
      businessId: null,
      originalName: 'delivery.pdf',
      contentType: 'application/pdf',
      sizeBytes: 1024,
      uploaderId: '2',
      uploaderName: '负责人',
      canDelete: true,
      createdAt: '2026-08-13T08:00:00Z',
    } satisfies AttachmentRecord
    const wrapper = shallowMount(DeliveryAcceptancePanel, {
      props: {
        snapshot: snapshot({ canSubmitDelivery: true }),
        pendingAttachments: [pendingAttachment],
      },
      global: { stubs },
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createDeliveryMock).toHaveBeenCalledWith(
      '100',
      expect.objectContaining({ attachmentIds: ['31'] }),
    )
  })

  it('需求方可以验收最新交付', async () => {
    const result = { requestVersion: 9 } as CreatedAcceptanceResult
    createAcceptanceMock.mockResolvedValue(result)
    const wrapper = shallowMount(DeliveryAcceptancePanel, {
      props: {
        snapshot: snapshot({
          requestStatus: 'PENDING_ACCEPTANCE',
          canAccept: true,
          deliveries: [
            {
              id: '11',
              requestId: '100',
              submitterId: '2',
              submitterName: '负责人',
              description: '已经部署并提交使用说明',
              deliveryUrl: null,
              attachments: [],
              createdAt: '2026-08-12T08:00:00Z',
            },
          ],
        }),
      },
      global: { stubs },
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createAcceptanceMock).toHaveBeenCalledWith('100', {
      requestVersion: 7,
      result: 'ACCEPTED',
      comment: null,
    })
    expect(wrapper.emitted('updated')).toEqual([[result]])
  })

  it('409 冲突时抛出 conflict 且不会抛出 updated', async () => {
    createDeliveryMock.mockRejectedValue({ isAxiosError: true, response: { status: 409 } })
    const wrapper = shallowMount(DeliveryAcceptancePanel, {
      props: { snapshot: snapshot({ canSubmitDelivery: true }) },
      global: { stubs },
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.emitted('conflict')).toHaveLength(1)
    expect(wrapper.emitted('updated')).toBeUndefined()
  })
})
