import { flushPromises, RouterLinkStub, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductLogo from '@/components/common/ProductLogo.vue'
import LandingIllustration from '@/components/public/LandingIllustration.vue'
import LandingView from './LandingView.vue'
import { getRegistrationStatus } from '@/api/auth'

vi.mock('@/api/auth', () => ({ getRegistrationStatus: vi.fn() }))

const getRegistrationStatusMock = vi.mocked(getRegistrationStatus)

describe('LandingView', () => {
  beforeEach(() => {
    getRegistrationStatusMock.mockResolvedValue({ enabled: true, emailSuffix: null })
  })

  it('以需求方语言展示服务范围、统一插画与完整协作流程', () => {
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.get('h1').text()).toContain('计算机技术组')
    expect(wrapper.text()).toContain('无需预先懂技术')
    expect(wrapper.text()).toContain('网站与系统建设')
    expect(wrapper.text()).toContain('专业可行性评估')
    expect(wrapper.findAll('.landing-workflow__steps li')).toHaveLength(5)
    expect(wrapper.findAll('.landing-feature-grid article')).toHaveLength(6)
    expect(
      wrapper.findAllComponents(LandingIllustration).map((item) => item.props('variant')),
    ).toEqual(['hero', 'collaboration', 'delivery'])
    expect(wrapper.findAllComponents(ProductLogo)).toHaveLength(2)
  })

  it('提供登录和注册路由入口', () => {
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    const destinations = wrapper.findAllComponents(RouterLinkStub).map((link) => link.props('to'))
    expect(destinations).toContain('/login')
    expect(destinations).toContain('/register')
  })

  it('自助注册关闭时把主入口调整为账号获取指引', async () => {
    getRegistrationStatusMock.mockResolvedValue({ enabled: false, emailSuffix: null })
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('先获取需求方账号')
    expect(wrapper.text()).toContain('当前采用受控开通方式')
    expect(wrapper.text()).not.toContain('开始描述我的需求')
  })

  it('大标题使用明确分行代替逗号断句', () => {
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    const splitTitles = wrapper.findAll('h2.landing-title-lines')

    expect(splitTitles).toHaveLength(7)
    splitTitles.forEach((title) => {
      const lines = title.findAll('span')

      expect(lines.length).toBeGreaterThanOrEqual(2)
      expect(lines.length).toBeLessThanOrEqual(3)
      lines.forEach((line) => expect(line.text().length).toBeLessThanOrEqual(9))
      expect(title.text()).not.toContain('，')
    })
  })

  it('移动导航按钮暴露展开状态并可切换', async () => {
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    const menuButton = wrapper.get('.landing-menu-button')

    expect(menuButton.attributes('aria-expanded')).toBe('false')
    await menuButton.trigger('click')
    expect(menuButton.attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('#landing-mobile-nav').exists()).toBe(true)
  })
})
