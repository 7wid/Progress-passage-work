import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import App from './App.vue'

describe('App', () => {
  it('根路由直接渲染，不使用会先卸载旧页面的阻塞式转场', () => {
    const wrapper = shallowMount(App, {
      global: {
        stubs: {
          RouterView: { template: '<main data-test="root-router-view" />' },
        },
      },
    })

    expect(wrapper.get('[data-test="root-router-view"]').element.tagName).toBe('MAIN')
    expect(wrapper.find('transition-stub').exists()).toBe(false)
  })
})
