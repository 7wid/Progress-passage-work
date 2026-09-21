<script setup lang="ts">
import { ArrowUpRight, CalendarDays, UserRound } from '@lucide/vue'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import type { RequestSummary } from '@/types/request'

defineProps<{ items: RequestSummary[] }>()
defineEmits<{ open: [id: string] }>()
const urgencyLabels = { NORMAL: '一般', HIGH: '较急', URGENT: '紧急' }
</script>

<template>
  <ul class="request-collection" aria-label="需求摘要">
    <li v-for="item in items" :key="item.id">
      <button type="button" class="request-summary" @click="$emit('open', item.id)">
        <span class="request-summary__top">
          <code>{{ item.requestNo ?? '未提交草稿' }}</code>
          <RequestStatusTag :status="item.status" />
        </span>
        <strong class="request-summary__title">{{ item.title || '未命名需求' }}</strong>
        <span class="request-summary__category">
          {{ item.categoryName || '未分类' }}
          <span v-if="item.urgency && item.urgency !== 'NORMAL'" class="request-summary__urgency">
            {{ urgencyLabels[item.urgency] }}
          </span>
        </span>
        <span class="request-summary__progress">
          <span
            >处理进度 <b>{{ item.progress }}%</b></span
          >
          <span class="request-summary__track" aria-hidden="true"
            ><i
              :style="{ transform: `scaleX(${Math.max(0, Math.min(100, item.progress)) / 100})` }"
          /></span>
        </span>
        <span class="request-summary__footer">
          <span><UserRound :size="14" aria-hidden="true" />{{ item.creatorName }}</span>
          <span
            ><CalendarDays :size="14" aria-hidden="true" />{{
              item.expectedDeadline ?? '未设置日期'
            }}</span
          >
          <ArrowUpRight :size="18" aria-hidden="true" />
        </span>
      </button>
    </li>
  </ul>
</template>

<style scoped>
.request-collection {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  list-style: none;
  margin: 0;
  padding: 20px;
}
.request-collection li {
  min-width: 0;
}
.request-summary {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 22px;
  text-align: left;
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition:
    border-color var(--motion-fast),
    box-shadow var(--motion-base);
}
.request-summary:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-raised);
}
.request-summary__top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.request-summary code {
  color: var(--color-text-tertiary);
  font: 12px var(--font-mono);
  overflow-wrap: anywhere;
}
.request-summary__title {
  font-size: 18px;
  line-height: 1.6;
  margin: 18px 0 8px;
  overflow-wrap: anywhere;
}
.request-summary__category {
  display: flex;
  gap: 12px;
  color: var(--color-text-secondary);
  font-size: 13px;
}
.request-summary__urgency {
  color: var(--color-warning-strong);
  font-weight: 650;
}
.request-summary__progress {
  display: grid;
  gap: 8px;
  margin-top: auto;
  padding-top: 26px;
}
.request-summary__progress > span:first-child {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.request-summary__progress b {
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.request-summary__track {
  display: block;
  height: 4px;
  background: var(--color-surface-secondary);
  border-radius: 4px;
  overflow: hidden;
}
.request-summary__track i {
  display: block;
  height: 100%;
  background: var(--color-primary);
  transform-origin: left;
  transition: transform 300ms var(--ease-standard);
}
.request-summary__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
  font-size: 12px;
}
.request-summary__footer > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  overflow-wrap: anywhere;
}
.request-summary__footer > svg {
  margin-left: auto;
  color: var(--color-primary);
}
@media (max-width: 700px) {
  .request-collection {
    grid-template-columns: 1fr;
    padding: 12px;
    gap: 12px;
  }
  .request-summary {
    padding: 18px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .request-summary,
  .request-summary__track i {
    transition: none;
  }
}
</style>
