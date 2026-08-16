<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRequests } from '@/api/requests'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { RequestListQuery, RequestSummary, RequestUrgency } from '@/types/request'

type WorkspaceQueue =
  | 'PENDING_REVIEW'
  | 'PENDING_ASSIGNMENT'
  | 'OWNER'
  | 'PARTICIPANT'
  | 'PENDING_ACCEPTANCE'
  | 'OVERDUE'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const activeQueue = ref<WorkspaceQueue>('PENDING_REVIEW')
const loading = ref(false)
const errorMessage = ref('')
const items = ref<RequestSummary[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
let loadSequence = 0

const queueLabels: Record<WorkspaceQueue, string> = {
  PENDING_REVIEW: '待评估',
  PENDING_ASSIGNMENT: '待分配',
  OWNER: '我负责',
  PARTICIPANT: '我参与',
  PENDING_ACCEPTANCE: '待验收',
  OVERDUE: '已逾期',
}

const urgencyMap: Record<RequestUrgency, { label: string; type: 'info' | 'warning' | 'danger' }> = {
  NORMAL: { label: '一般', type: 'info' },
  HIGH: { label: '较急', type: 'warning' },
  URGENT: { label: '紧急', type: 'danger' },
}

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatDateTime(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function queueQuery(): RequestListQuery {
  const base: RequestListQuery = {
    page: page.value,
    pageSize,
    sort: activeQueue.value === 'OVERDUE' ? 'DEADLINE_ASC' : 'NEWEST',
  }
  switch (activeQueue.value) {
    case 'PENDING_REVIEW':
    case 'PENDING_ASSIGNMENT':
    case 'PENDING_ACCEPTANCE':
      return { ...base, status: activeQueue.value }
    case 'OWNER':
      return { ...base, assignmentType: 'OWNER', activeOnly: true }
    case 'PARTICIPANT':
      return { ...base, assignmentType: 'PARTICIPANT', activeOnly: true }
    case 'OVERDUE':
      return { ...base, activeOnly: true, overdue: true }
  }
}

async function loadQueue() {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getRequests(queueQuery())
    if (sequence !== loadSequence) return
    items.value = result.items
    total.value = result.total
  } catch {
    if (sequence !== loadSequence) return
    items.value = []
    total.value = 0
    errorMessage.value = `${queueLabels[activeQueue.value]}需求加载失败，请稍后重试`
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function openDetail(id: string) {
  void router.push({
    name: 'request-detail',
    params: { id },
    query: { from: 'workspace' },
  })
}

function changePage(value: number) {
  page.value = value
  void loadQueue()
}

watch(activeQueue, () => {
  page.value = 1
  void loadQueue()
})

onMounted(loadQueue)
</script>

<template>
  <section class="page">
    <div class="page__header">
      <div>
        <h2>技术组工作台</h2>
        <span class="summary">{{ queueLabels[activeQueue] }} {{ total }} 条</span>
      </div>
      <el-button :loading="loading" @click="loadQueue">刷新</el-button>
    </div>

    <el-tabs v-model="activeQueue">
      <el-tab-pane label="待评估" name="PENDING_REVIEW" />
      <el-tab-pane v-if="isAdmin" label="待分配" name="PENDING_ASSIGNMENT" />
      <el-tab-pane label="我负责" name="OWNER" />
      <el-tab-pane label="我参与" name="PARTICIPANT" />
      <el-tab-pane label="待验收" name="PENDING_ACCEPTANCE" />
      <el-tab-pane label="已逾期" name="OVERDUE" />
    </el-tabs>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadQueue">重新加载</el-button>
      </template>
    </el-alert>

    <el-card>
      <el-table v-loading="loading" :data="items" row-key="id" empty-text="当前队列暂无需求">
        <el-table-column label="需求编号" width="180">
          <template #default="{ row }">{{ row.requestNo ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.urgency" :type="urgencyMap[row.urgency as RequestUrgency].type">
              {{ urgencyMap[row.urgency as RequestUrgency].label }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><RequestStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="期望日期" width="120">
          <template #default="{ row }">{{ row.expectedDeadline ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > pageSize"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="changePage"
      />
    </el-card>
  </section>
</template>

<style scoped>
.summary {
  color: #6b7280;
}
.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
