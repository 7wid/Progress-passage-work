<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getEnabledCategories } from '@/api/categories'
import { getRequests } from '@/api/requests'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  CategoryOption,
  RequestSort,
  RequestStatus,
  RequestSummary,
  RequestUrgency,
} from '@/types/request'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const items = ref<RequestSummary[]>([])
const categories = ref<CategoryOption[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
let loadSequence = 0

const filters = reactive<{
  keyword: string
  status?: RequestStatus
  categoryId?: string
  submittedRange: string[]
  sort: RequestSort
}>({
  keyword: '',
  status: undefined,
  categoryId: undefined,
  submittedRange: [],
  sort: 'NEWEST',
})

const statusOptions: Array<{ label: string; value: RequestStatus }> = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待评估', value: 'PENDING_REVIEW' },
  { label: '待补充', value: 'NEED_MORE_INFO' },
  { label: '待分配', value: 'PENDING_ASSIGNMENT' },
  { label: '处理中', value: 'IN_PROGRESS' },
  { label: '待验收', value: 'PENDING_ACCEPTANCE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
]

const sortOptions: Array<{ label: string; value: RequestSort }> = [
  { label: '最新提交', value: 'NEWEST' },
  { label: '最早提交', value: 'OLDEST' },
  { label: '截止日期升序', value: 'DEADLINE_ASC' },
  { label: '截止日期降序', value: 'DEADLINE_DESC' },
]

const urgencyMap: Record<RequestUrgency, { label: string; type: 'info' | 'warning' | 'danger' }> = {
  NORMAL: { label: '一般', type: 'info' },
  HIGH: { label: '较急', type: 'warning' },
  URGENT: { label: '紧急', type: 'danger' },
}

const canCreate = computed(
  () => authStore.user?.role === 'REQUESTER' || authStore.user?.role === 'ADMIN',
)

const pageTitle = computed(() => (authStore.user?.role === 'REQUESTER' ? '我的需求' : '需求池'))

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatDateTime(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

async function loadCategories() {
  try {
    categories.value = await getEnabledCategories()
  } catch {
    ElMessage.warning('分类加载失败，仍可查看全部需求')
  }
}

async function loadData() {
  const currentSequence = ++loadSequence
  loading.value = true

  try {
    const result = await getRequests({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword,
      status: filters.status,
      categoryId: filters.categoryId,
      submittedFrom: filters.submittedRange[0],
      submittedTo: filters.submittedRange[1],
      sort: filters.sort,
    })

    if (currentSequence !== loadSequence) return

    items.value = result.items
    total.value = result.total
  } catch {
    if (currentSequence === loadSequence) {
      items.value = []
      total.value = 0
      ElMessage.error('需求列表加载失败，请稍后重试')
    }
  } finally {
    if (currentSequence === loadSequence) loading.value = false
  }
}

function search() {
  page.value = 1
  void loadData()
}

function reset() {
  filters.keyword = ''
  filters.status = undefined
  filters.categoryId = undefined
  filters.submittedRange = []
  filters.sort = 'NEWEST'
  page.value = 1
  void loadData()
}

function handleCurrentChange(value: number) {
  page.value = value
  void loadData()
}

function handleSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
  void loadData()
}

function openCreate() {
  void router.push({ name: 'request-create' })
}

function openDetail(id: string) {
  void router.push({ name: 'request-detail', params: { id } })
}

onMounted(() => {
  void loadCategories()
  void loadData()
})
</script>

<template>
  <section class="page">
    <div class="page__header">
      <h2>{{ pageTitle }}</h2>
      <el-button v-if="canCreate" type="primary" @click="openCreate">提交需求</el-button>
    </div>

    <el-card>
      <el-form class="filters" label-position="top" @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="需求编号或标题"
            clearable
            maxlength="80"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" clearable placeholder="全部分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="提交日期">
          <el-date-picker
            v-model="filters.submittedRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>

        <el-form-item label="排序">
          <el-select v-model="filters.sort">
            <el-option
              v-for="option in sortOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <div class="filters__actions">
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card>
      <el-table v-loading="loading" :data="items" row-key="id" empty-text="暂无需求">
        <el-table-column label="需求编号" width="180">
          <template #default="{ row }">{{ row.requestNo ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="130" />
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="urgencyMap[row.urgency as RequestUrgency].type">
              {{ urgencyMap[row.urgency as RequestUrgency].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><RequestStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="进度" width="150">
          <template #default="{ row }"><el-progress :percentage="row.progress" /></template>
        </el-table-column>
        <el-table-column prop="expectedDeadline" label="期望日期" width="120" />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </section>
</template>

<style scoped>
.filters {
  display: grid;
  grid-template-columns: minmax(180px, 1.2fr) repeat(4, minmax(150px, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.filters :deep(.el-select),
.filters :deep(.el-date-editor) {
  width: 100%;
}

.filters__actions {
  display: flex;
  padding-bottom: 1px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1100px) {
  .filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
