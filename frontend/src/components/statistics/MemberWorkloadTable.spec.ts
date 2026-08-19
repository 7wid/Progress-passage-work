import { defineComponent, h, inject, provide, type PropType } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MemberWorkloadTable from './MemberWorkloadTable.vue'
import type { MemberWorkload } from '@/types/statistics'

const tableRowsKey = Symbol('tableRows')

const TableStub = defineComponent({
  props: { data: { type: Array as PropType<MemberWorkload[]>, default: () => [] } },
  setup(props, { slots }) {
    provide(tableRowsKey, props)
    return () => h('div', props.data.length ? slots.default?.() : slots.empty?.())
  },
})

const ColumnStub = defineComponent({
  setup(_, { slots }) {
    const props = inject<{ data: MemberWorkload[] }>(tableRowsKey)
    return () => h('div', [slots.header?.(), props?.data.map((row) => slots.default?.({ row }))])
  },
})

function mountTable(data: MemberWorkload[]) {
  return mount(MemberWorkloadTable, {
    props: { data },
    global: {
      directives: { loading: () => undefined },
      stubs: {
        'el-empty': { props: ['description'], template: '<p>{{ description }}</p>' },
        'el-table': TableStub,
        'el-table-column': ColumnStub,
      },
    },
  })
}

describe('MemberWorkloadTable', () => {
  it('展示成员身份与精确负载数量', () => {
    const wrapper = mountTable([
      {
        memberId: '5',
        memberAccount: 'member-a',
        memberName: '成员甲',
        activeCount: 3,
        inProgressCount: 2,
        pendingAcceptanceCount: 1,
      },
    ])

    expect(wrapper.text()).toContain('成员甲')
    expect(wrapper.text()).toContain('@member-a')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.get('[role="region"]').attributes('aria-label')).toBe('成员负载明细')
    expect(wrapper.get('[role="region"]').attributes('tabindex')).toBe('0')
  })

  it('无在岗成员时展示明确空态', () => {
    const wrapper = mountTable([])
    expect(wrapper.text()).toContain('当前筛选范围内暂无在岗成员')
  })

  it('排序表头可用键盘按钮切换方向并更新表格顺序', async () => {
    const wrapper = mountTable([
      {
        memberId: '5',
        memberAccount: 'member-a',
        memberName: '成员甲',
        activeCount: 3,
        inProgressCount: 1,
        pendingAcceptanceCount: 2,
      },
      {
        memberId: '6',
        memberAccount: 'member-b',
        memberName: '成员乙',
        activeCount: 1,
        inProgressCount: 1,
        pendingAcceptanceCount: 0,
      },
    ])
    const table = wrapper.findComponent(TableStub)

    expect((table.props('data') as MemberWorkload[]).map((item) => item.memberId)).toEqual([
      '5',
      '6',
    ])
    await wrapper.get('button[aria-label="按当前负载升序排列"]').trigger('click')
    expect((table.props('data') as MemberWorkload[]).map((item) => item.memberId)).toEqual([
      '6',
      '5',
    ])
    expect(wrapper.text()).toContain('已按当前负载升序排列')
  })
})
