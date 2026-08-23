import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LandingIllustration from './LandingIllustration.vue'

describe('LandingIllustration', () => {
  it.each([
    ['hero', '需求从问题描述到成果确认的产品流程插画'],
    ['collaboration', '资料、沟通与处理结论集中协作的插画'],
    ['delivery', '在需求协作中心查看交付进度和确认成果的插画'],
  ] as const)('使用无人物的代码原生产品场景：%s', (variant, title) => {
    const wrapper = mount(LandingIllustration, { props: { variant } })

    expect(wrapper.get('title').text()).toBe(title)
    expect(wrapper.find('image').exists()).toBe(false)
    expect(wrapper.find('[class*="person"]').exists()).toBe(false)
    expect(wrapper.find('[class*="skin"]').exists()).toBe(false)
  })
})
