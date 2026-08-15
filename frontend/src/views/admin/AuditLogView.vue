<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import { getAdminAuditLogs } from '@/api/auditLogs'
import { getApiErrorMessage } from '@/api/http'
import type { AuditJson, AuditLogRecord } from '@/types/audit'

const BUSINESS_TIME_ZONE = 'Asia/Shanghai'
const actionOptions = [
  ['AUTH_LOGIN', '登录成功'],
  ['AUTH_LOGIN_FAILED', '登录失败'],
  ['AUTH_LOGOUT', '退出登录'],
  ['REQUEST_SUBMITTED', '提交需求'],
  ['EVALUATION_CREATED', '提交评估'],
  ['EVALUATION_REJECTION_CONFIRMED', '确认不承接'],
  ['ASSIGNMENT_UPDATE', '调整任务成员'],
  ['PROGRESS_RECORDED', '发布进度'],
  ['DELIVERY_SUBMITTED', '提交交付'],
  ['ACCEPTANCE_RECORDED', '提交验收'],
  ['MEMBER_CREATE', '创建成员'],
  ['MEMBER_UPDATE', '更新成员'],
  ['MEMBER_STATUS', '变更成员状态'],
  ['CATEGORY_CREATE', '创建分类'],
  ['CATEGORY_UPDATE', '更新分类'],
  ['CATEGORY_STATUS', '变更分类状态'],
  ['REQUEST_CANCEL', '取消需求'],
  ['REQUEST_REOPEN', '重新开启需求'],
] as const

const actionLabels = Object.fromEntries(actionOptions) as Record<string, string>
const targetLabels: Record<string, string> = {
  USER: '用户',
  REQUEST: '需求',
  CATEGORY: '分类',
  AUTHENTICATION: '认证',
}

function businessDateParts(): { year: string; month: string; day: string } {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const value = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? ''
  return { year: value('year'), month: value('month'), day: value('day') }
}

function currentMonthRange(): [string, string] {
  const { year, month, day } = businessDateParts()
  return [`${year}-${month}-01`, `${year}-${month}-${day}`]
}

const loading = ref(false)
const errorMessage = ref('')
const items = shallowRef<AuditLogRecord[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selected = shallowRef<AuditLogRecord | null>(null)
const detailVisible = ref(false)
const filters = reactive({
  dateRange: currentMonthRange(),
  actorId: '',
  action: '',
  targetType: '',
  targetId: '',
  requestId: '',
})
let loadSequence = 0

const hasDetails = computed(
  () => selected.value?.beforeData != null || selected.value?.afterData != null,
)
const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'medium',
})

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function formatJson(value: AuditJson): string {
  return value == null ? '无' : JSON.stringify(value, null, 2)
}

function actionLabel(action: string): string {
  return actionLabels[action] ?? action
}

function targetLabel(targetType: string): string {
  return targetLabels[targetType] ?? targetType
}

async function loadData(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getAdminAuditLogs({
      page: page.value,
      pageSize: pageSize.value,
      actorId: filters.actorId,
      action: filters.action,
      targetType: filters.targetType,
      targetId: filters.targetId,
      requestId: filters.requestId,
      from: filters.dateRange[0],
      to: filters.dateRange[1],
    })
    if (sequence !== loadSequence) return
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (sequence !== loadSequence) return
    items.value = []
    total.value = 0
    errorMessage.value = getApiErrorMessage(error, '审计记录加载失败，请稍后重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function search(): void {
  page.value = 1
  void loadData()
}

function resetFilters(): void {
  filters.dateRange = currentMonthRange()
  filters.actorId = ''
  filters.action = ''
  filters.targetType = ''
  filters.targetId = ''
  filters.requestId = ''
  search()
}

function changePage(value: number): void {
  page.value = value
  void loadData()
}

function changePageSize(value: number): void {
  pageSize.value = value
  page.value = 1
  void loadData()
}

function openDetail(item: AuditLogRecord): void {
  selected.value = item
  detailVisible.value = true
}

onMounted(() => void loadData())
</script>

<template>
  <section class="page">
    <div class="page__header">
      <div>
        <h2>审计记录</h2>
        <span class="summary">关键操作只读追踪；敏感字段由服务端统一脱敏</span>
      </div>
      <el-tag type="info">仅管理员可见</el-tag>
    </div>

    <el-card>
      <el-form label-position="top" @submit.prevent="search">
        <div class="filters">
          <el-form-item label="操作日期">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :clearable="false"
            />
          </el-form-item>
          <el-form-item label="动作">
            <el-select v-model="filters.action" clearable filterable placeholder="全部动作">
              <el-option
                v-for="option in actionOptions"
                :key="option[0]"
                :label="option[1]"
                :value="option[0]"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="目标类型">
            <el-select v-model="filters.targetType" clearable placeholder="全部类型">
              <el-option label="用户" value="USER" />
              <el-option label="需求" value="REQUEST" />
              <el-option label="分类" value="CATEGORY" />
              <el-option label="认证" value="AUTHENTICATION" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作者 ID">
            <el-input v-model="filters.actorId" maxlength="20" placeholder="精确 ID" />
          </el-form-item>
          <el-form-item label="目标 ID">
            <el-input v-model="filters.targetId" maxlength="80" placeholder="精确 ID" />
          </el-form-item>
          <el-form-item label="请求 ID">
            <el-input v-model="filters.requestId" maxlength="80" placeholder="链路追踪 ID" />
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置为本月</el-button>
          <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
        </div>
      </el-form>
    </el-card>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadData">重新加载</el-button>
      </template>
    </el-alert>

    <el-card>
      <el-table v-loading="loading" :data="items" row-key="id" empty-text="暂无审计记录">
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作者" min-width="150">
          <template #default="{ row }">
            {{ row.actorName }}<small v-if="row.actorId">（{{ row.actorId }}）</small>
          </template>
        </el-table-column>
        <el-table-column label="动作" min-width="170">
          <template #default="{ row }">{{ actionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column label="目标" min-width="150">
          <template #default="{ row }">
            {{ targetLabel(row.targetType) }}{{ row.targetId ? ` #${row.targetId}` : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="requestId" label="请求 ID" min-width="190" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="来源 IP" width="150" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 0"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="changePage"
        @size-change="changePageSize"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="审计详情" width="min(760px, 92vw)">
      <template v-if="selected">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="动作">{{
            actionLabel(selected.action)
          }}</el-descriptions-item>
          <el-descriptions-item label="发生时间">
            {{ formatDateTime(selected.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="请求 ID" :span="2">
            {{ selected.requestId ?? '—' }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="hasDetails" class="snapshot-grid">
          <section>
            <h3>变更前</h3>
            <pre>{{ formatJson(selected.beforeData) }}</pre>
          </section>
          <section>
            <h3>变更后</h3>
            <pre>{{ formatJson(selected.afterData) }}</pre>
          </section>
        </div>
        <el-empty v-else description="本次操作没有数据快照" />
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.summary,
small {
  color: #6b7280;
}
.filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}
.filters :deep(.el-date-editor),
.filters :deep(.el-select) {
  width: 100%;
}
.filter-actions,
.pagination {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.pagination {
  margin-top: 16px;
}
.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}
.snapshot-grid h3 {
  margin: 0 0 8px;
  font-size: 15px;
}
.snapshot-grid pre {
  min-height: 120px;
  max-height: 360px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 6px;
  color: #d1d5db;
  background: #111827;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
@media (max-width: 760px) {
  .filters,
  .snapshot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
