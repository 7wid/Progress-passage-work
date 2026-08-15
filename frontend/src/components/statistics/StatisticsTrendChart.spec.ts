import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatisticsTrendChart from './StatisticsTrendChart.vue'

describe('StatisticsTrendChart', () => {
  it('无非零数据时展示明确空态', () => {
    const wrapper = mount(StatisticsTrendChart, {
      props: { data: [{ date: '2026-08-01', count: 0 }] },
    })
    expect(wrapper.text()).toContain('暂无新增需求')
    expect(wrapper.find('svg').exists()).toBe(false)
  })

  it('为趋势点提供可访问的日期和数量说明', () => {
    const wrapper = mount(StatisticsTrendChart, {
      props: {
        data: [
          { date: '2026-08-01', count: 1 },
          { date: '2026-08-02', count: 3 },
        ],
      },
    })
    expect(wrapper.get('svg').attributes('aria-label')).toBe('需求新增趋势折线图')
    expect(wrapper.text()).toContain('2026-08-02：3 条')
  })
})
