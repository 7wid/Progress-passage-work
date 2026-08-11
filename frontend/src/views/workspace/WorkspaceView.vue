<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRequests } from '@/api/requests'
import { useAuthStore } from '@/stores/auth'
import type { RequestSummary, RequestUrgency } from '@/types/request'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

type WorkspaceStatus = 'PENDING_REVIEW' | 'PENDING_ASSIGNMENT'

const activeStatus = ref<WorkspaceStatus>('PENDING_REVIEW')

const loading = ref(false)
const errorMessage = ref('')
const items = ref<RequestSummary[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
let loadSequence = 0

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
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getRequests({
      page: page.value,
      pageSize,
      status: activeStatus.value,
      sort: 'DEADLINE_ASC',
    })

    if (sequence !== loadSequence) return
    items.value = result.items
    total.value = result.total
  } catch {
    if (sequence !== loadSequence) return
    items.value = []
    total.value = 0

    errorMessage.value =
      activeStatus.value === 'PENDING_REVIEW'
        ? '待评估需求加载失败，请确认后端已经启动'
        : '待分配需求加载失败，请确认后端已经启动'
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function openDetail(id: string) {
  void router.push({
    name: 'request-detail',
    params: {
      id,
    },
    query: {
      from: 'workspace',
    },
  })
}

function changePage(value: number) {
  page.value = value

  void loadPendingRequests()
}

/*
 * 监听工作台页签。
 *
 * 待评估：
 *   PENDING_REVIEW
 *
 * 待分配：
 *   PENDING_ASSIGNMENT
 *
 * 每次切换页签以后：
 *
 * 1. 页码恢复为第一页
 * 2. 根据新的 activeStatus 重新查询后端
 */
watch(activeStatus, () => {
  page.value = 1
  void loadPendingRequests()
})

onMounted(() => {
  void loadPendingRequests()
})
</script>

<template>
  <section class="page">
    <!-- ===================================================== -->
    <!-- 工作台头部 -->
    <!-- ===================================================== -->

    <div class="page__header">
      <div>
        <h2>技术组工作台</h2>

        <span class="summary">
          当前共有
          {{ total }}
          条
          {{ activeStatus === 'PENDING_REVIEW' ? '待评估' : '待分配' }}
          需求
        </span>
      </div>

      <el-button :loading="loading" @click="loadPendingRequests"> 刷新 </el-button>
    </div>

    <!-- ===================================================== -->
    <!-- 工作队列切换 -->
    <!--
      PENDING_REVIEW
          ↓
      待评估

      PENDING_ASSIGNMENT
          ↓
      待分配
    -->
    <!-- ===================================================== -->

    <el-tabs v-model="activeStatus">
      <el-tab-pane label="待评估" name="PENDING_REVIEW" />

      <el-tab-pane v-if="isAdmin" label="待分配" name="PENDING_ASSIGNMENT" />
    </el-tabs>

    <!-- ===================================================== -->
    <!-- 加载错误 -->
    <!-- ===================================================== -->

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadPendingRequests"> 重新加载 </el-button>
      </template>
    </el-alert>

    <!-- ===================================================== -->
    <!-- 工作队列表格 -->
    <!-- ===================================================== -->

    <el-card>
      <el-table
        v-loading="loading"
        :data="items"
        row-key="id"
        :empty-text="activeStatus === 'PENDING_REVIEW' ? '暂无待评估需求' : '暂无待分配需求'"
      >
        <!-- 需求编号 -->

        <el-table-column label="需求编号" width="180">
          <template #default="{ row }">
            {{ row.requestNo ?? '—' }}
          </template>
        </el-table-column>

        <!-- 标题 -->

        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />

        <!-- 分类 -->

        <el-table-column prop="categoryName" label="分类" width="140" />

        <!-- 创建人 -->

        <el-table-column prop="creatorName" label="创建人" width="120" />

        <!-- 紧急程度 -->

        <el-table-column label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="urgencyMap[row.urgency as RequestUrgency].type">
              {{ urgencyMap[row.urgency as RequestUrgency].label }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 期望日期 -->

        <el-table-column prop="expectedDeadline" label="期望日期" width="120" />

        <!-- 提交时间 -->

        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>

        <!-- ================================================= -->
        <!-- 操作 -->
        <!--
          待评估：
              查看并评估

          待分配：
              查看并分配
        -->
        <!-- ================================================= -->

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">
              {{
                activeStatus === 'PENDING_REVIEW'
                  ? '查看并评估'
                  : isAdmin
                    ? '查看并分配'
                    : '查看详情'
              }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- =================================================== -->
      <!-- 分页 -->
      <!-- =================================================== -->

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
