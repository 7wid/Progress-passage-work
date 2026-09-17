import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'
import LandingRequestDemo from './LandingRequestDemo.vue'
import { landingScenarios } from './landingScenarios'

const wrappers: ReturnType<typeof mount>[] = []
function renderDemo() {
  const wrapper = mount(LandingRequestDemo, {
    attachTo: document.body,
    props: { scenarioId: 'website' },
    global: { stubs: { transition: true } },
  })
  wrappers.push(wrapper)
  return wrapper
}
afterEach(() => wrappers.splice(0).forEach((wrapper) => wrapper.unmount()))

describe('LandingRequestDemo', () => {
  it('展示完整五步体验，支持退回调整、验收和重新开始', async () => {
    const wrapper = renderDemo()
    expect(wrapper.text()).toContain('不会提交真实需求')
    expect(wrapper.findAll('.request-demo__steps li')).toHaveLength(5)
    await wrapper.get('.request-demo__next').trigger('click')
    expect(wrapper.text()).toContain('活动流程与报名字段.pdf')
    await wrapper.get('.request-demo__next').trigger('click')
    expect(wrapper.text()).toContain('真实需求是否承接，以团队评估结果为准')
    await wrapper.get('.request-demo__next').trigger('click')
    expect(wrapper.text()).toContain('正在搭建可预览版本')
    await wrapper.get('.request-demo__next').trigger('click')
    expect(wrapper.get('.request-scene__status').text()).toBe('待验收')
    await wrapper.get('.request-demo__adjust').trigger('click')
    expect(document.activeElement).toBe(wrapper.get('.request-demo__next').element)
    expect(wrapper.get('.request-scene__status').text()).toBe('处理中')
    expect(wrapper.text()).toContain('根据你的反馈继续调整')
    expect(wrapper.find('.request-demo__adjust').exists()).toBe(false)
    await wrapper.get('.request-demo__next').trigger('click')
    await wrapper.get('.request-demo__next').trigger('click')
    expect(wrapper.get('.request-scene__status').text()).toBe('已验收')
    expect(wrapper.get('[role="status"]').text()).toContain('验收完成')
    await wrapper.get('.request-demo__next').trigger('click')
    expect(wrapper.get('.request-scene__status').text()).toBe('整理需求')
  })

  it('重新开始后保留可继续操作的键盘焦点', async () => {
    const wrapper = renderDemo()
    await wrapper.get('.request-demo__next').trigger('click')
    await wrapper.get('.request-demo__reset').trigger('click')
    expect(wrapper.get('.request-scene__status').text()).toBe('整理需求')
    expect(document.activeElement).toBe(wrapper.get('.request-demo__next').element)
  })

  it.each(landingScenarios)('切换到 $label 时清除旧场景进度与验收状态', async (scenario) => {
    const wrapper = renderDemo()
    const buttons = wrapper.findAll('.request-demo__scenarios button')
    const target = buttons.find((button) => button.text() === scenario.label)!
    await target.trigger('click')
    expect(wrapper.emitted('update:scenarioId')?.[0]).toEqual([scenario.id])
    for (let i = 0; i < 5; i += 1) await wrapper.get('.request-demo__next').trigger('click')
    const id = scenario.id === 'website' ? 'data' : scenario.id
    await wrapper.setProps({ scenarioId: id })
    expect(wrapper.get('.request-scene__status').text()).toBe('整理需求')
    expect(wrapper.find('.request-demo__adjust').exists()).toBe(false)
    expect(wrapper.get('.request-demo__steps [aria-current="step"]').text()).toContain('描述')
    expect(wrapper.find('.is-accepted').exists()).toBe(false)
  })
})
