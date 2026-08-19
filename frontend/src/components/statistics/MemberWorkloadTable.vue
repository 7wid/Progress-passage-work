<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown, ArrowUp, ArrowUpDown } from '@lucide/vue'
import type { MemberWorkload } from '@/types/statistics'

const props = withDefaults(defineProps<{ data: MemberWorkload[]; loading?: boolean }>(), {
  loading: false,
})

const maximum = computed(() => Math.max(1, ...props.data.map((item) => item.activeCount)))
type WorkloadSortKey = 'activeCount' | 'inProgressCount' | 'pendingAcceptanceCount'
type SortDirection = 'ascending' | 'descending'

const sortKey = ref<WorkloadSortKey>('activeCount')
const sortDirection = ref<SortDirection>('descending')
const sortLabels: Record<WorkloadSortKey, string> = {
  activeCount: '当前负载',
  inProgressCount: '处理中',
  pendingAcceptanceCount: '待验收',
}
const sortedData = computed(() =>
  [...props.data].sort((left, right) => {
    const difference = left[sortKey.value] - right[sortKey.value]
    if (difference !== 0) return sortDirection.value === 'ascending' ? difference : -difference
    return left.memberName.localeCompare(right.memberName, 'zh-CN')
  }),
)
const sortAnnouncement = computed(
  () =>
    `已按${sortLabels[sortKey.value]}${sortDirection.value === 'ascending' ? '升序' : '降序'}排列`,
)

function meterWidth(count: number): string {
  return count === 0 ? '0%' : `${Math.max(4, (count / maximum.value) * 100)}%`
}

function toggleSort(key: WorkloadSortKey): void {
  if (sortKey.value === key) {
    sortDirection.value = sortDirection.value === 'ascending' ? 'descending' : 'ascending'
    return
  }
  sortKey.value = key
  sortDirection.value = 'descending'
}

function sortActionLabel(key: WorkloadSortKey): string {
  if (sortKey.value !== key) return `按${sortLabels[key]}降序排列`
  return `按${sortLabels[key]}${sortDirection.value === 'ascending' ? '降序' : '升序'}排列`
}
</script>

<template>
  <div
    v-loading="loading"
    class="workload-table-region"
    role="region"
    aria-label="成员负载明细"
    tabindex="0"
    :aria-busy="loading"
    element-loading-text="成员负载加载中"
  >
    <span class="visually-hidden" role="status" aria-live="polite">{{ sortAnnouncement }}</span>
    <el-table :data="sortedData" row-key="memberId">
      <el-table-column label="成员" min-width="190">
        <template #default="{ row }: { row: MemberWorkload }">
          <div class="member-cell">
            <strong>{{ row.memberName }}</strong>
            <span>@{{ row.memberAccount }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="activeCount" min-width="210">
        <template #header>
          <button
            class="sort-button"
            type="button"
            :aria-label="sortActionLabel('activeCount')"
            :aria-pressed="sortKey === 'activeCount'"
            @click="toggleSort('activeCount')"
          >
            <span>当前负载</span>
            <ArrowDown
              v-if="sortKey === 'activeCount' && sortDirection === 'descending'"
              :size="15"
              aria-hidden="true"
            />
            <ArrowUp v-else-if="sortKey === 'activeCount'" :size="15" aria-hidden="true" />
            <ArrowUpDown v-else :size="15" aria-hidden="true" />
          </button>
        </template>
        <template #default="{ row }: { row: MemberWorkload }">
          <div class="workload-cell">
            <strong>{{ row.activeCount }}</strong>
            <span class="workload-meter" aria-hidden="true">
              <span :style="{ width: meterWidth(row.activeCount) }" />
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="inProgressCount" min-width="120" align="right">
        <template #header>
          <button
            class="sort-button sort-button--right"
            type="button"
            :aria-label="sortActionLabel('inProgressCount')"
            :aria-pressed="sortKey === 'inProgressCount'"
            @click="toggleSort('inProgressCount')"
          >
            <span>处理中</span>
            <ArrowDown
              v-if="sortKey === 'inProgressCount' && sortDirection === 'descending'"
              :size="15"
              aria-hidden="true"
            />
            <ArrowUp v-else-if="sortKey === 'inProgressCount'" :size="15" aria-hidden="true" />
            <ArrowUpDown v-else :size="15" aria-hidden="true" />
          </button>
        </template>
      </el-table-column>
      <el-table-column prop="pendingAcceptanceCount" min-width="120" align="right">
        <template #header>
          <button
            class="sort-button sort-button--right"
            type="button"
            :aria-label="sortActionLabel('pendingAcceptanceCount')"
            :aria-pressed="sortKey === 'pendingAcceptanceCount'"
            @click="toggleSort('pendingAcceptanceCount')"
          >
            <span>待验收</span>
            <ArrowDown
              v-if="sortKey === 'pendingAcceptanceCount' && sortDirection === 'descending'"
              :size="15"
              aria-hidden="true"
            />
            <ArrowUp
              v-else-if="sortKey === 'pendingAcceptanceCount'"
              :size="15"
              aria-hidden="true"
            />
            <ArrowUpDown v-else :size="15" aria-hidden="true" />
          </button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="当前筛选范围内暂无在岗成员" :image-size="72" />
      </template>
    </el-table>
  </div>
</template>

<style scoped>
.workload-table-region {
  overflow-x: auto;
  border-radius: var(--radius-md);
  outline: none;
}
.workload-table-region:focus-visible {
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.sort-button {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 5px;
  padding: 0;
  color: inherit;
  background: transparent;
  border: 0;
  font: inherit;
  cursor: pointer;
}
.sort-button--right {
  width: 100%;
  justify-content: flex-end;
}
.sort-button:focus-visible {
  border-radius: var(--radius-sm);
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
.member-cell {
  display: grid;
  min-width: 0;
  gap: 2px;
}
.member-cell strong {
  overflow-wrap: anywhere;
  color: var(--color-text-primary);
}
.member-cell span {
  overflow-wrap: anywhere;
  color: var(--color-text-tertiary);
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}
.workload-cell {
  display: grid;
  grid-template-columns: 36px minmax(100px, 1fr);
  align-items: center;
  gap: 10px;
  font-variant-numeric: tabular-nums;
}
.workload-cell strong {
  color: var(--color-text-primary);
  text-align: right;
}
.workload-meter {
  display: block;
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-surface-secondary);
}
.workload-meter span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
  transition: width 180ms var(--ease-standard);
}

@media (prefers-reduced-motion: reduce) {
  .workload-meter span {
    transition: none;
  }
}
</style>
