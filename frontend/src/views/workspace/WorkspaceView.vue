<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRequests } from '@/api/requests'
import type { RequestSummary, RequestUrgency } from '@/types/request'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const items = ref<RequestSummary[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)

const urgencyMap: Record<
  RequestUrgency,
  {
    label: string
    type: 'info' | 'warning' | 'danger'
  }
> = {
  NORMAL: {
    label: '一般',
    type: 'info',
  },
  HIGH: {
    label: '较急',
    type: 'warning',
  },
  URGENT: {
    label: '紧急',
    type: 'danger',
  },
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

async function loadPendingRequests() {
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getRequests({
      page: page.value,
      pageSize,
      status: 'PENDING_REVIEW',
      sort: 'DEADLINE_ASC',
    })

    items.value = result.items
    total.value = result.total
  } catch {
    items.value = []
    total.value = 0
    errorMessage.value = '待评估需求加载失败，请确认后端已经启动'
  } finally {
    loading.value = false
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
  void loadPendingRequests()
}

onMounted(() => {
  void loadPendingRequests()
})
</script>

<template>
  <section class="page">
    <div class="page__header">
      <div>
        <h2>技术组工作台</h2>
        <span class="summary"> 当前共有 {{ total }} 条待评估需求 </span>
      </div>

      <el-button :loading="loading" @click="loadPendingRequests"> 刷新 </el-button>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadPendingRequests"> 重新加载 </el-button>
      </template>
    </el-alert>

    <el-card>
      <el-table v-loading="loading" :data="items" row-key="id" empty-text="暂无待评估需求">
        <el-table-column label="需求编号" width="180">
          <template #default="{ row }">
            {{ row.requestNo ?? '—' }}
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />

        <el-table-column prop="categoryName" label="分类" width="140" />

        <el-table-column prop="creatorName" label="创建人" width="120" />

        <el-table-column label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="urgencyMap[row.urgency as RequestUrgency].type">
              {{ urgencyMap[row.urgency as RequestUrgency].label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="expectedDeadline" label="期望日期" width="120" />

        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)"> 查看并评估 </el-button>
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
