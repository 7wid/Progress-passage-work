import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ProductLogo from './ProductLogo.vue'

describe('ProductLogo', () => {
  it('统一渲染宣传页使用的 ClipboardCheck 品牌图形', () => {
    const wrapper = mount(ProductLogo, { props: { size: 24, strokeWidth: 1.8 } })
    const logo = wrapper.get('svg')

    expect(logo.classes()).toContain('lucide-clipboard-check')
    expect(logo.attributes('width')).toBe('24')
    expect(logo.attributes('height')).toBe('24')
    expect(logo.attributes('stroke-width')).toBe('1.8')
  })
})
