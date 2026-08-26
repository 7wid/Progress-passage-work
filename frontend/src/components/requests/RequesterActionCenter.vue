<script setup lang="ts">
import { computed, useId } from 'vue'
import {
  ArrowRight,
  BadgeCheck,
  CircleCheckBig,
  FilePenLine,
  MessageSquareWarning,
  PackageCheck,
  Plus,
} from '@lucide/vue'
import type { Component } from 'vue'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import type { RequestStatus, RequestSummary } from '@/types/request'

type ActionStatus = 'DRAFT' | 'NEED_MORE_INFO' | 'PENDING_ACCEPTANCE'

interface ActionMeta {
  eyebrow: string
  description: string
  actionLabel: string
  icon: Component
  tone: 'neutral' | 'warning' | 'success'
}

const props = defineProps<{
  items: RequestSummary[]
  total: number
  loading?: boolean
}>()

const titleId = useId()

const actionMeta: Record<ActionStatus, ActionMeta> = {
  DRAFT: {
    eyebrow: '尚未提交',
    description: '完善信息并提交后，技术组才会开始评估。',
    actionLabel: '继续填写',
    icon: FilePenLine,
    tone: 'neutral',
  },
  NEED_MORE_INFO: {
    eyebrow: '需要补充资料',
    description: '查看评估意见，补全信息后重新提交。',
    actionLabel: '补充资料',
    icon: MessageSquareWarning,
    tone: 'warning',
  },
  PENDING_ACCEPTANCE: {
    eyebrow: '成果等待确认',
    description: '检查交付内容，确认通过或提出调整意见。',
    actionLabel: '查看并验收',
    icon: PackageCheck,
    tone: 'success',
  },
}

const visibleItems = computed(() => props.items.slice(0, 5))

function isActionStatus(status: RequestStatus): status is ActionStatus {
  return status === 'DRAFT' || status === 'NEED_MORE_INFO' || status === 'PENDING_ACCEPTANCE'
}

function metaFor(status: RequestStatus): ActionMeta {
  return isActionStatus(status) ? actionMeta[status] : actionMeta.DRAFT
}

function actionTarget(item: RequestSummary) {
  if (item.status === 'DRAFT' || item.status === 'NEED_MORE_INFO') {
    return { name: 'request-edit', params: { id: item.id } }
  }
  return { name: 'request-detail', params: { id: item.id }, hash: '#delivery-acceptance' }
}
</script>

<template>
  <section class="action-center" :aria-labelledby="titleId" :aria-busy="loading">
    <header class="action-center__header">
      <div>
        <span class="action-center__eyebrow">
          <BadgeCheck :size="15" aria-hidden="true" /> 需求方待办
        </span>
        <h2 :id="titleId">待我处理</h2>
        <p>优先完成这些事项，需求才能继续向下一阶段推进。</p>
      </div>
      <span class="action-center__count" aria-live="polite">
        <strong>{{ total }}</strong>
        <span>项待办</span>
      </span>
    </header>

    <div v-if="loading" class="action-center__loading" aria-label="正在加载需求方待办">
      <el-skeleton :rows="2" animated />
    </div>

    <div v-else-if="visibleItems.length" class="action-center__list">
      <article
        v-for="item in visibleItems"
        :key="item.id"
        class="action-item"
        :class="`action-item--${metaFor(item.status).tone}`"
      >
        <span class="action-item__icon" aria-hidden="true">
          <component :is="metaFor(item.status).icon" :size="20" :stroke-width="1.9" />
        </span>
        <div class="action-item__content">
          <div class="action-item__meta">
            <span>{{ metaFor(item.status).eyebrow }}</span>
            <code>{{ item.requestNo ?? '草稿' }}</code>
            <RequestStatusTag :status="item.status" />
          </div>
          <strong>{{ item.title || '未命名需求' }}</strong>
          <p>{{ metaFor(item.status).description }}</p>
        </div>
        <RouterLink class="action-item__link" :to="actionTarget(item)">
          {{ metaFor(item.status).actionLabel }}
          <ArrowRight :size="16" aria-hidden="true" />
        </RouterLink>
      </article>
      <RouterLink v-if="total > visibleItems.length" class="action-center__more" to="/requests">
        还有 {{ total - visibleItems.length }} 项待办，前往我的需求查看
        <ArrowRight :size="16" aria-hidden="true" />
      </RouterLink>
    </div>

    <div v-else class="action-center__empty">
      <span class="action-center__empty-icon" aria-hidden="true">
        <CircleCheckBig :size="24" :stroke-width="1.8" />
      </span>
      <div>
        <strong>目前没有需要你操作的事项</strong>
        <p>已提交的需求会由技术组继续推进；有补充或验收任务时会显示在这里。</p>
      </div>
      <RouterLink class="action-center__create" to="/requests/new">
        <Plus :size="16" aria-hidden="true" />
        发起新需求
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.action-center {
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.action-center__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 22px;
  background: linear-gradient(90deg, var(--color-primary-soft), var(--color-surface) 62%);
  border-bottom: 1px solid var(--color-border-subtle);
}

.action-center__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.action-center__header h2 {
  margin: 5px 0 0;
  color: var(--color-text-primary);
  font-size: 21px;
  line-height: 1.25;
}

.action-center__header p,
.action-center__empty p,
.action-item__content p {
  margin: 5px 0 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.action-center__count {
  display: grid;
  min-width: 72px;
  justify-items: end;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.action-center__count strong {
  color: var(--color-primary-strong);
  font-family: var(--font-mono);
  font-size: 28px;
  line-height: 1;
}

.action-center__loading {
  padding: 20px 22px;
}

.action-center__list {
  display: grid;
}

.action-item {
  --action-color: var(--color-text-secondary);
  --action-soft: var(--color-surface-muted);
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 15px;
  min-height: 92px;
  padding: 16px 22px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.action-item--warning {
  --action-color: var(--color-warning-strong);
  --action-soft: var(--color-warning-soft);
}

.action-item--success {
  --action-color: var(--color-success-strong);
  --action-soft: var(--color-success-soft);
}

.action-item__icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--action-color);
  background: var(--action-soft);
  border-radius: 10px;
}

.action-item__content {
  min-width: 0;
}

.action-item__content > strong {
  display: block;
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-item__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 5px;
  color: var(--action-color);
  font-size: 12px;
  font-weight: 650;
}

.action-item__meta code {
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
}

.action-item__link,
.action-center__more,
.action-center__create {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 40px;
  color: var(--color-primary-strong);
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
  border-radius: 8px;
  transition:
    color 180ms var(--ease-standard),
    background-color 180ms var(--ease-standard),
    box-shadow 180ms var(--ease-standard);
}

.action-item__link {
  padding: 0 12px;
  border: 1px solid var(--color-primary-border);
}

.action-item__link:hover,
.action-item__link:focus-visible {
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
}

.action-item__link:focus-visible,
.action-center__more:focus-visible,
.action-center__create:focus-visible {
  outline: 3px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.action-center__more {
  justify-self: center;
  margin: 10px;
  padding: 0 14px;
}

.action-center__more:hover {
  background: var(--color-primary-soft);
}

.action-center__empty {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 20px 22px;
}

.action-center__empty-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--color-success-strong);
  background: var(--color-success-soft);
  border-radius: 50%;
}

.action-center__empty strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.action-center__create {
  padding: 0 14px;
  color: white;
  background: var(--color-primary);
}

.action-center__create:hover {
  color: white;
  background: var(--color-primary-strong);
  box-shadow: var(--shadow-sm);
}

@media (max-width: 720px) {
  .action-center__header,
  .action-center__empty {
    align-items: start;
    padding: 18px;
  }

  .action-item {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 12px;
    padding: 16px 18px;
  }

  .action-item__link {
    grid-column: 2;
    justify-self: start;
  }

  .action-center__empty {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .action-center__create {
    grid-column: 1 / -1;
    width: 100%;
    min-height: 44px;
  }
}

@media (max-width: 440px) {
  .action-center__header {
    gap: 12px;
  }

  .action-center__header p {
    max-width: 24ch;
  }

  .action-center__count {
    min-width: 56px;
  }

  .action-center__count strong {
    font-size: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .action-item__link,
  .action-center__more,
  .action-center__create {
    transition: none;
  }
}
</style>
