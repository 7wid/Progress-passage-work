import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RequesterJourneyPanel from './RequesterJourneyPanel.vue'

describe('RequesterJourneyPanel', () => {
  it('在待验收状态明确提示需求方处理交付成果', () => {
    const wrapper = mount(RequesterJourneyPanel, {
      props: { status: 'PENDING_ACCEPTANCE' },
    })

    expect(wrapper.text()).toContain('成果已提交，正在等待验收')
    expect(wrapper.text()).toContain('检查交付内容')
    expect(wrapper.find('[aria-current="step"]').text()).toContain('交付验收')
    expect(wrapper.findAll('.is-completed')).toHaveLength(3)
  })

  it('在待补充状态说明服务团队进展与下一步操作', () => {
    const wrapper = mount(RequesterJourneyPanel, {
      props: { status: 'NEED_MORE_INFO' },
    })

    expect(wrapper.classes()).toContain('journey-panel--attention')
    expect(wrapper.text()).toContain('服务团队已完成初步核对')
    expect(wrapper.text()).toContain('查看评估意见并补充需求内容')
  })

  it('已完成时将全部流程节点标记为完成', () => {
    const wrapper = mount(RequesterJourneyPanel, {
      props: { status: 'COMPLETED' },
    })

    expect(wrapper.classes()).toContain('journey-panel--success')
    expect(wrapper.findAll('.is-completed')).toHaveLength(5)
    expect(wrapper.text()).toContain('需求已验收并完成归档')
  })

  it('流程终止时不猜测历史阶段', () => {
    const wrapper = mount(RequesterJourneyPanel, {
      props: { status: 'REJECTED' },
    })

    expect(wrapper.find('.journey-panel__track').exists()).toBe(false)
    expect(wrapper.text()).toContain('标准处理流程已停止')
    expect(wrapper.text()).toContain('查看下方评估意见')
  })

  it('多个实例使用独立的可访问标题标识', () => {
    const wrapper = mount({
      components: { RequesterJourneyPanel },
      template: `
        <div>
          <RequesterJourneyPanel status="IN_PROGRESS" />
          <RequesterJourneyPanel status="COMPLETED" />
        </div>
      `,
    })

    const labelIds = wrapper
      .findAll('.journey-panel')
      .map((panel) => panel.attributes('aria-labelledby'))
    expect(new Set(labelIds).size).toBe(2)
  })
})
