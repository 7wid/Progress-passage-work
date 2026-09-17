import { flushPromises, RouterLinkStub, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getRegistrationStatus } from '@/api/auth'
import ProductLogo from '@/components/common/ProductLogo.vue'
import LandingJourney from '@/components/public/LandingJourney.vue'
import LandingRequestDemo from '@/components/public/LandingRequestDemo.vue'
import LandingView from './LandingView.vue'

vi.mock('@/api/auth', () => ({ getRegistrationStatus: vi.fn() }))
const getRegistrationStatusMock = vi.mocked(getRegistrationStatus)
const wrappers: ReturnType<typeof shallowMount>[] = []

function renderPage() {
  const wrapper = shallowMount(LandingView, {
    attachTo: document.body,
    global: { stubs: { RouterLink: RouterLinkStub, transition: true } },
  })
  wrappers.push(wrapper)
  return wrapper
}

describe('LandingView', () => {
  beforeEach(() => {
    getRegistrationStatusMock.mockResolvedValue({ enabled: true, emailSuffix: null })
  })
  afterEach(() => {
    wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
    vi.restoreAllMocks()
  })

  it('展示服务场景、统一品牌、登录入口和可访问的主要内容', async () => {
    const wrapper = renderPage()
    await flushPromises()
    expect(wrapper.get('h1').text()).toContain('计算机技术组')
    expect(wrapper.text()).toContain('无需预先懂技术')
    expect(wrapper.findAll('.landing-services__choices button')).toHaveLength(4)
    expect(wrapper.findAllComponents(ProductLogo)).toHaveLength(2)
    expect(wrapper.get('.skip-link').attributes('href')).toBe('#landing-main')
    expect(wrapper.get('main').attributes('tabindex')).toBe('-1')
    expect(wrapper.findAll('.landing-faq__list details')).toHaveLength(4)
    const routes = wrapper.findAllComponents(RouterLinkStub).map((link) => link.props('to'))
    expect(routes).toContain('/login')
    expect(routes).toContain('/register')
    expect(wrapper.text()).toContain('开始描述我的需求')
  })

  it('场景选择与首屏演示、协作流程双向联动', async () => {
    const wrapper = renderPage()
    const choice = wrapper.findAll('.landing-services__choices button')[2]!
    await choice.trigger('click')
    expect(choice.attributes('aria-pressed')).toBe('true')
    expect(wrapper.findComponent(LandingRequestDemo).props('scenarioId')).toBe('data')
    expect(wrapper.findComponent(LandingJourney).props('scenarioId')).toBe('data')
    expect(wrapper.get('.landing-services__example').text()).toContain('一份清楚、可核对的数据结果')
    wrapper.findComponent(LandingRequestDemo).vm.$emit('update:scenarioId', 'support')
    await wrapper.vm.$nextTick()
    expect(
      wrapper.findAll('.landing-services__choices button')[3]!.attributes('aria-pressed'),
    ).toBe('true')
    expect(wrapper.findComponent(LandingJourney).props('scenarioId')).toBe('support')
  })

  it('注册状态尚未返回时不承诺开放注册', () => {
    getRegistrationStatusMock.mockReturnValue(new Promise(() => {}))
    const wrapper = renderPage()
    expect(wrapper.text()).toContain('了解如何开始')
    expect(wrapper.text()).not.toContain('开始描述我的需求')
    expect(wrapper.text()).not.toContain('提交新需求')
  })

  it.each(['disabled', 'unavailable'])(
    '注册关闭或请求失败时提供账号获取指引：%s',
    async (state) => {
      if (state === 'disabled')
        getRegistrationStatusMock.mockResolvedValue({ enabled: false, emailSuffix: null })
      else getRegistrationStatusMock.mockRejectedValue(new Error('network unavailable'))
      const wrapper = renderPage()
      await flushPromises()
      expect(wrapper.text()).toContain('先获取需求方账号')
      expect(wrapper.text()).toContain('当前采用受控开通方式')
      expect(wrapper.text()).not.toContain('开始描述我的需求')
    },
  )

  it('移动导航支持 Escape 关闭并返回按钮焦点，锚点点击后收起', async () => {
    const wrapper = renderPage()
    const button = wrapper.get('.landing-menu-button')
    await button.trigger('click')
    expect(button.attributes('aria-expanded')).toBe('true')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()
    expect(button.attributes('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(button.element)
    await button.trigger('click')
    await wrapper.get('#landing-mobile-nav a').trigger('click')
    expect(wrapper.find('#landing-mobile-nav').exists()).toBe(false)
  })
})
