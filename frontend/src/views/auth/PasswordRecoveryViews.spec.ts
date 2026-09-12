import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ElButton, ElForm, ElFormItem, ElInput, ElSkeleton } from 'element-plus'
import ForgotPasswordView from './ForgotPasswordView.vue'
import ResetPasswordView from './ResetPasswordView.vue'
import { getPasswordRecoveryStatus, requestPasswordReset, resetPassword } from '@/api/auth'

const { route, replace, resetSession } = vi.hoisted(() => ({
  route: { path: '/reset-password', hash: '', query: {} },
  replace: vi.fn().mockResolvedValue(undefined),
  resetSession: vi.fn(),
}))
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ replace }) }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ $resetSession: resetSession }) }))
vi.mock('@/api/auth', () => ({
  getPasswordRecoveryStatus: vi.fn(),
  requestPasswordReset: vi.fn(),
  resetPassword: vi.fn(),
}))

const global = {
  components: { ElButton, ElForm, ElFormItem, ElInput, ElSkeleton },
  stubs: {
    AuthLayout: { template: '<main><slot /></main>' },
    RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
  },
}
let wrapper: VueWrapper | undefined

beforeEach(() => {
  vi.clearAllMocks()
  HTMLElement.prototype.scrollIntoView = vi.fn()
  vi.mocked(getPasswordRecoveryStatus).mockResolvedValue({ enabled: true, channel: 'EMAIL' })
  vi.mocked(requestPasswordReset).mockResolvedValue(undefined)
  vi.mocked(resetPassword).mockResolvedValue(undefined)
  route.hash = `#token=${'a'.repeat(43)}`
})
afterEach(() => {
  wrapper?.unmount()
})

describe('找回密码', () => {
  it('服务关闭时提供联系管理员的路径', async () => {
    vi.mocked(getPasswordRecoveryStatus).mockResolvedValue({ enabled: false, channel: 'EMAIL' })
    wrapper = mount(ForgotPasswordView, { global })
    await flushPromises()
    expect(wrapper.text()).toContain('联系管理员')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.find('a').attributes('href')).toBe('/login')
  })

  it('读取状态失败后可以重试', async () => {
    vi.mocked(getPasswordRecoveryStatus).mockRejectedValueOnce(new Error('offline'))
    wrapper = mount(ForgotPasswordView, { global })
    await flushPromises()
    expect(wrapper.text()).toContain('重新加载')
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('无效邮箱不发送邮件，合法邮箱显示统一反馈并阻止连续提交', async () => {
    wrapper = mount(ForgotPasswordView, { global })
    await flushPromises()
    await wrapper.get('input').setValue('invalid')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(requestPasswordReset).not.toHaveBeenCalled()
    await vi.waitFor(() => expect(wrapper!.text()).toContain('请输入有效邮箱'))
    await wrapper.get('input').setValue('user@example.org')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(requestPasswordReset).toHaveBeenCalledWith('user@example.org')
    expect(wrapper.text()).toContain('如果该邮箱已绑定可用账号')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    await wrapper.get('form').trigger('submit')
    expect(requestPasswordReset).toHaveBeenCalledTimes(1)
  })
})

describe('重置密码', () => {
  it('缺少凭证时提供重新申请入口', async () => {
    route.hash = ''
    wrapper = mount(ResetPasswordView, { global })
    await flushPromises()
    expect(wrapper.text()).toContain('重置链接无效或已过期')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(resetPassword).not.toHaveBeenCalled()
  })

  it('移除地址栏凭证，校验确认密码并在成功后回到登录', async () => {
    wrapper = mount(ResetPasswordView, { global })
    await flushPromises()
    expect(replace).toHaveBeenCalledWith({ path: '/reset-password', query: {}, hash: '' })
    const inputs = wrapper.findAll('input')
    await inputs[0]!.setValue(' NewPassword123 ')
    await inputs[1]!.setValue('different')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(resetPassword).not.toHaveBeenCalled()
    await vi.waitFor(() => expect(wrapper!.text()).toContain('两次输入的密码不一致'))
    await inputs[1]!.setValue(' NewPassword123 ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(resetPassword).toHaveBeenCalledWith('a'.repeat(43), ' NewPassword123 ')
    expect(resetSession).toHaveBeenCalledOnce()
    expect(replace).toHaveBeenLastCalledWith({ path: '/login', query: { passwordReset: '1' } })
  })

  it('服务器拒绝过期凭证后不能继续提交', async () => {
    vi.mocked(resetPassword).mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 400,
        data: { error: { code: 'INVALID_RESET_TOKEN', message: '链接已失效' } },
      },
    })
    wrapper = mount(ResetPasswordView, { global })
    await flushPromises()
    for (const input of wrapper.findAll('input')) await input.setValue('NewPassword123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('重新申请重置邮件')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(resetSession).not.toHaveBeenCalled()
  })
})
