import { RouterLinkStub, shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LandingView from './LandingView.vue'

describe('LandingView', () => {
  it('展示技术组背景、系统能力与完整协作流程', () => {
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.get('h1').text()).toContain('计算机技术组')
    expect(wrapper.text()).toContain('网站与系统建设')
    expect(wrapper.text()).toContain('专业可行性评估')
    expect(wrapper.findAll('.landing-workflow__steps li')).toHaveLength(5)
    expect(wrapper.findAll('.landing-feature-grid article')).toHaveLength(6)
  })

  it('提供登录和注册路由入口', () => {
    const wrapper = shallowMount(LandingView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    const destinations = wrapper.findAllComponents(RouterLinkStub).map((link) => link.props('to'))
    expect(destinations).toContain('/login')
    expect(destinations).toContain('/register')
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
