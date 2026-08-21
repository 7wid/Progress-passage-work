<script setup lang="ts">
import { computed, markRaw, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import {
  Activity,
  ArrowRight,
  CheckCircle2,
  ChevronRight,
  CircleDot,
  ClipboardClock,
  Clock3,
  ListFilter,
  Plus,
  RefreshCw,
} from '@lucide/vue'
import { useRouter } from 'vue-router'
import { getRequests } from '@/api/requests'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { RequestStatus, RequestSummary } from '@/types/request'

interface MetricConfig {
  label: string
  status: RequestStatus
  tone: string
  hint: string
  icon: Component
}

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const total = ref(0)
const recent = ref<RequestSummary[]>([])
const counts = ref<Partial<Record<RequestStatus, number>>>({})

const requesterCards: MetricConfig[] = [
  {
    label: '待评估',
    status: 'PENDING_REVIEW',
    tone: 'blue',
    hint: '服务团队正在评估',
    icon: markRaw(ClipboardClock),
  },
  {
    label: '处理中',
    status: 'IN_PROGRESS',
    tone: 'green',
    hint: '正在协作推进',
    icon: markRaw(CircleDot),
  },
  {
    label: '待验收',
    status: 'PENDING_ACCEPTANCE',
    tone: 'orange',
    hint: '等待确认成果',
    icon: markRaw(Clock3),
  },
  {
    label: '已完成',
    status: 'COMPLETED',
    tone: 'purple',
    hint: '已闭环交付',
    icon: markRaw(CheckCircle2),
  },
]

const teamCards: MetricConfig[] = [
  {
    label: '待评估',
    status: 'PENDING_REVIEW',
    tone: 'blue',
    hint: '需要给出可行性结论',
    icon: markRaw(ClipboardClock),
  },
  {
    label: '待分配',
    status: 'PENDING_ASSIGNMENT',
    tone: 'orange',
    hint: '等待明确负责人',
    icon: markRaw(Clock3),
  },
  {
    label: '处理中',
    status: 'IN_PROGRESS',
    tone: 'green',
    hint: '当前执行中的任务',
    icon: markRaw(CircleDot),
  },
  {
    label: '待验收',
    status: 'PENDING_ACCEPTANCE',
    tone: 'purple',
    hint: '成果已提交待确认',
    icon: markRaw(CheckCircle2),
  },
]

const cards = computed(() => (authStore.user?.role === 'REQUESTER' ? requesterCards : teamCards))
const heading = computed(() =>
  authStore.user?.role === 'REQUESTER' ? '我的需求进展' : '服务需求概览',
)
const introDescription = computed(() =>
  authStore.user?.role === 'REQUESTER'
    ? '从需求发起到成果验收，在这里查看每一步进展。'
    : '集中查看待响应、待分配与执行中的服务需求。',
)
const activeLabel = computed(() => (authStore.user?.role === 'REQUESTER' ? '进行中' : '待推进'))
const recentTitle = computed(() => (authStore.user?.role === 'REQUESTER' ? '最近更新' : '最近需求'))
const recentDescription = computed(() =>
  authStore.user?.role === 'REQUESTER' ? '关注状态发生变化的需求' : '按最近更新时间排列',
)
const canCreate = computed(
  () => authStore.user?.role === 'REQUESTER' || authStore.user?.role === 'ADMIN',
)
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())
const activeTotal = computed(() =>
  cards.value
    .filter((card) => card.status !== 'COMPLETED')
    .reduce((sum, card) => sum + (counts.value[card.status] ?? 0), 0),
)

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [latest, ...statusResults] = await Promise.all([
      getRequests({ page: 1, pageSize: 5, sort: 'NEWEST' }),
      ...cards.value.map((card) =>
        getRequests({ page: 1, pageSize: 1, status: card.status, sort: 'NEWEST' }),
      ),
    ])
    total.value = latest.total
    recent.value = latest.items
    counts.value = Object.fromEntries(
      cards.value.map((card, index) => [card.status, statusResults[index]?.total ?? 0]),
    )
  } catch {
    total.value = 0
    recent.value = []
    counts.value = {}
    errorMessage.value = '首页数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function openStatus(status: RequestStatus) {
  void router.push({ name: 'request-list', query: { status } })
}

function openRequest(id: string) {
  void router.push({ name: 'request-detail', params: { id } })
}

onMounted(loadDashboard)
</script>

<template>
  <section class="page dashboard-page">
    <header class="dashboard-intro">
      <div class="dashboard-intro__copy">
        <span class="dashboard-intro__date">
          <Activity :size="14" aria-hidden="true" />
          {{ today }}
        </span>
        <h1>{{ greeting }}，{{ authStore.user?.displayName }}</h1>
        <p>{{ introDescription }}</p>
      </div>
      <dl class="dashboard-intro__signals" aria-label="工作摘要">
        <div>
          <dt>{{ activeLabel }}</dt>
          <dd>{{ activeTotal }}</dd>
        </div>
        <div>
          <dt>全部需求</dt>
          <dd>{{ total }}</dd>
        </div>
      </dl>
      <div class="dashboard-intro__actions">
        <el-button :loading="loading" @click="loadDashboard">
          <RefreshCw :size="17" aria-hidden="true" />
          刷新数据
        </el-button>
        <el-button v-if="canCreate" type="primary" @click="router.push('/requests/new')">
          <Plus :size="17" aria-hidden="true" />
          发起新需求
        </el-button>
      </div>
    </header>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <div class="overview-heading">
      <div>
        <h2>{{ heading }}</h2>
        <span class="total">共 {{ total }} 条需求</span>
      </div>
      <button type="button" class="text-action" @click="router.push('/requests')">
        查看全部
        <ArrowRight :size="16" aria-hidden="true" />
      </button>
    </div>

    <div v-loading="loading" class="metrics" aria-label="需求状态概览">
      <button
        v-for="card in cards"
        :key="card.status"
        type="button"
        class="metric"
        :class="`metric--${card.tone}`"
        :aria-label="`${card.label} ${counts[card.status] ?? 0} 条，${card.hint}`"
        @click="openStatus(card.status)"
      >
        <span class="metric__icon" aria-hidden="true">
          <component :is="card.icon" :size="19" :stroke-width="1.8" />
        </span>
        <span class="metric__content">
          <span>{{ card.label }}</span>
          <small>{{ card.hint }}</small>
        </span>
        <strong>{{ counts[card.status] ?? 0 }}</strong>
        <ChevronRight :size="17" class="metric__arrow" aria-hidden="true" />
      </button>
    </div>

    <el-card class="recent-card">
      <template #header>
        <div class="card-heading">
          <div>
            <strong>{{ recentTitle }}</strong>
            <span>{{ recentDescription }}</span>
          </div>
          <ListFilter :size="18" aria-hidden="true" />
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="recent"
        row-key="id"
        empty-text="暂无需求"
        class="recent-table"
        @row-click="(row: RequestSummary) => openRequest(row.id)"
      >
        <el-table-column label="需求编号" width="170">
          <template #default="{ row }">{{ row.requestNo ?? '草稿' }}</template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <button type="button" class="request-title-link" @click.stop="openRequest(row.id)">
              {{ row.title }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><RequestStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="期望日期" width="120">
          <template #default="{ row }">{{ row.expectedDeadline ?? '—' }}</template>
        </el-table-column>
      </el-table>

      <div v-loading="loading" class="recent-mobile" aria-label="最近需求">
        <button
          v-for="item in recent"
          :key="item.id"
          type="button"
          class="recent-mobile__item"
          @click="openRequest(item.id)"
        >
          <span class="recent-mobile__main">
            <span class="recent-mobile__meta">
              <code>{{ item.requestNo ?? '草稿' }}</code>
              <RequestStatusTag :status="item.status" />
            </span>
            <strong>{{ item.title }}</strong>
            <small
              >{{ item.categoryName || '未分类' }} ·
              {{ item.expectedDeadline ?? '未设置日期' }}</small
            >
          </span>
          <ChevronRight :size="18" aria-hidden="true" />
        </button>
        <div v-if="!loading && !recent.length" class="recent-mobile__empty">暂无需求</div>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.dashboard-intro {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: end;
  gap: 36px;
  padding: 4px 0 28px;
  border-bottom: 1px solid var(--color-border);
  animation: intro-enter 380ms var(--ease-standard) both;
}

.dashboard-intro__copy {
  max-width: 660px;
}

.dashboard-intro__date {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 9px;
  color: var(--color-primary-strong);
  font-size: 13px;
  font-weight: 600;
}

.dashboard-intro h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 31px;
  font-weight: 680;
  line-height: 1.2;
}

.dashboard-intro p {
  margin: 9px 0 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.dashboard-intro__signals {
  display: flex;
  align-items: center;
  gap: 0;
  margin: 0;
}

.dashboard-intro__signals > div {
  display: grid;
  min-width: 94px;
  gap: 3px;
  padding: 2px 20px;
  border-left: 1px solid var(--color-border);
}

.dashboard-intro__signals dt {
  color: var(--color-text-tertiary);
  font-size: 11px;
  font-weight: 600;
}

.dashboard-intro__signals dd {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 25px;
  font-weight: 680;
  font-variant-numeric: tabular-nums;
  line-height: 1.15;
}

.dashboard-intro__actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.dashboard-intro__actions :deep(.el-button) {
  min-height: 42px;
  margin: 0;
}

.dashboard-intro__actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.overview-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: -8px;
}

.overview-heading h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 19px;
  font-weight: 650;
}

.total {
  display: block;
  margin-top: 3px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.text-action {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  color: var(--color-primary-strong);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
}

.text-action:hover {
  background: var(--color-primary-soft);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-height: 102px;
}

.metric {
  display: grid;
  min-width: 0;
  min-height: 102px;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 17px;
  text-align: left;
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease,
    background-color var(--motion-fast) ease;
  animation: metric-enter 360ms var(--ease-standard) both;
}

.metric:nth-child(2) {
  animation-delay: 45ms;
}

.metric:nth-child(3) {
  animation-delay: 90ms;
}

.metric:nth-child(4) {
  animation-delay: 135ms;
}

.metric:hover {
  background: #ffffff;
  border-color: var(--metric-border, var(--color-border));
  box-shadow: var(--shadow-raised);
}

.metric__icon {
  display: inline-grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--metric-color, var(--color-text-secondary));
  background: var(--metric-background, var(--color-surface-secondary));
  border-radius: var(--radius-md);
}

.metric__content {
  display: grid;
  min-width: 0;
  gap: 2px;
  font-size: 14px;
  font-weight: 620;
}

.metric__content small {
  overflow: hidden;
  color: var(--color-text-tertiary);
  font-size: 11px;
  font-weight: 450;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric strong {
  color: var(--color-text-primary);
  font-size: 27px;
  font-weight: 680;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.metric__arrow {
  color: var(--color-text-tertiary);
  opacity: 0;
  transform: translateX(-3px);
  transition:
    color var(--motion-fast) ease,
    opacity var(--motion-fast) ease,
    transform var(--motion-fast) ease;
}

.metric:hover .metric__arrow,
.metric:focus-visible .metric__arrow {
  color: var(--metric-color);
  opacity: 1;
  transform: translateX(0);
}

.metric--blue {
  --metric-color: #2563eb;
  --metric-background: #eff6ff;
  --metric-border: #bfdbfe;
}

.metric--green {
  --metric-color: #059669;
  --metric-background: #ecfdf5;
  --metric-border: #a7f3d0;
}

.metric--orange {
  --metric-color: #d97706;
  --metric-background: #fffbeb;
  --metric-border: #fde68a;
}

.metric--purple {
  --metric-color: #7c3aed;
  --metric-background: #f5f3ff;
  --metric-border: #ddd6fe;
}

@keyframes intro-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}

@keyframes metric-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
}

.recent-card {
  min-width: 0;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--color-text-tertiary);
}

.card-heading > div {
  display: grid;
  gap: 2px;
}

.card-heading strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.card-heading span {
  font-size: 12px;
  font-weight: 450;
}

.recent-card :deep(.el-card__body) {
  padding: 0;
}

.recent-table :deep(.el-table__row) {
  cursor: pointer;
}

.request-title-link {
  display: block;
  max-width: 100%;
  padding: 4px 0;
  overflow: hidden;
  color: var(--color-text-primary);
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  font: inherit;
  font-weight: 580;
}

.request-title-link:hover {
  color: var(--color-primary-strong);
}

.recent-mobile {
  display: none;
}

@media (max-width: 1180px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .dashboard-intro {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: flex-start;
    gap: 20px;
  }

  .dashboard-intro__signals {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .dashboard-intro__signals > div:first-child {
    padding-left: 0;
    border-left: 0;
  }
}

@media (max-width: 560px) {
  .dashboard-intro {
    grid-template-columns: 1fr;
    padding-bottom: 22px;
  }

  .dashboard-intro h1 {
    font-size: 27px;
  }

  .dashboard-intro__signals {
    grid-column: auto;
    grid-row: auto;
  }

  .dashboard-intro__actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .dashboard-intro__actions :deep(.el-button) {
    flex: 1;
  }

  .metrics {
    grid-template-columns: 1fr;
  }

  .recent-table {
    display: none;
  }

  .recent-mobile {
    display: grid;
    min-height: 96px;
  }

  .recent-mobile__item {
    display: grid;
    min-width: 0;
    min-height: 92px;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 12px;
    padding: 15px 16px;
    color: var(--color-text-tertiary);
    text-align: left;
    background: var(--color-surface);
    border: 0;
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .recent-mobile__item:hover,
  .recent-mobile__item:focus-visible {
    background: var(--color-primary-soft);
  }

  .recent-mobile__main {
    display: grid;
    min-width: 0;
    gap: 5px;
  }

  .recent-mobile__meta {
    display: flex;
    min-width: 0;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .recent-mobile__meta code {
    overflow: hidden;
    color: var(--color-text-tertiary);
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 11px;
  }

  .recent-mobile__main > strong {
    overflow: hidden;
    color: var(--color-text-primary);
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 620;
  }

  .recent-mobile__main > small {
    overflow: hidden;
    color: var(--color-text-tertiary);
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
  }

  .recent-mobile__empty {
    display: grid;
    min-height: 120px;
    place-items: center;
    color: var(--color-text-tertiary);
    font-size: 13px;
  }
}
</style>
