<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RotateCcw, Search, UserPlus, UsersRound } from '@lucide/vue'
import {
  changeAdminRequesterStatus,
  createAdminRequester,
  getAdminRequesters,
} from '@/api/adminRequesters'
import { getApiErrorCode, getApiErrorMessage, getApiFieldErrors } from '@/api/http'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import AdminReasonDialog from '@/components/admin/AdminReasonDialog.vue'
import RequesterCreateDialog from '@/components/admin/RequesterCreateDialog.vue'
import type {
  AdminRequester,
  CreateAdminRequesterInput,
  RequesterAccountStatus,
} from '@/types/adminRequester'

const loading = ref(false)
const errorMessage = ref('')
const items = ref<AdminRequester[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = reactive<{ keyword: string; status?: RequesterAccountStatus }>({ keyword: '' })
let loadSequence = 0
const createVisible = ref(false)
const creating = ref(false)
const createError = ref('')
const createFieldErrors = ref<Record<string, string>>({})
const statusVisible = ref(false)
const statusTarget = ref<AdminRequester | null>(null)
const statusSubmitting = ref(false)
const statusReasonError = ref('')
const statusTitle = computed(() =>
  statusTarget.value?.status === 'ACTIVE' ? '停用需求方账号' : '启用需求方账号',
)
const statusDescription = computed(() => {
  const target = statusTarget.value
  if (!target) return ''
  return target.status === 'ACTIVE'
    ? `停用“${target.displayName}”后，现有登录会话将失效，无法提交、补充或验收需求。已有需求和记录会保留，请先确认业务交接。`
    : `启用“${target.displayName}”后，账号可重新登录，继续跟进原有需求；登录失败锁定也会解除。`
})
const dateFormatter = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' })
function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateFormatter.format(date)
}

async function loadRequesters(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getAdminRequesters({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword,
      status: filters.status,
    })
    if (sequence !== loadSequence) return
    if (result.items.length === 0 && page.value > 1) {
      page.value = Math.max(1, Math.ceil(result.total / pageSize.value))
      await loadRequesters()
      return
    }
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (sequence !== loadSequence) return
    items.value = []
    total.value = 0
    errorMessage.value = getApiErrorMessage(error, '需求方账号加载失败，请重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}
function search(): void {
  page.value = 1
  void loadRequesters()
}
function resetFilters(): void {
  filters.keyword = ''
  filters.status = undefined
  search()
}
function changePage(value: number): void {
  page.value = value
  void loadRequesters()
}
function changePageSize(value: number): void {
  pageSize.value = value
  search()
}
function openCreate(): void {
  createError.value = ''
  createFieldErrors.value = {}
  createVisible.value = true
}
async function submitCreate(input: CreateAdminRequesterInput): Promise<void> {
  if (creating.value) return
  creating.value = true
  createError.value = ''
  createFieldErrors.value = {}
  try {
    await createAdminRequester(input)
    createVisible.value = false
    ElMessage.success('需求方账号已开通，请安全告知初始密码并提醒登录后修改')
    filters.keyword = ''
    filters.status = undefined
    page.value = 1
    await loadRequesters()
  } catch (error) {
    createFieldErrors.value = getApiFieldErrors(error)
    createError.value = getApiErrorMessage(error, '账号开通失败，请检查填写内容')
  } finally {
    creating.value = false
  }
}
function openStatus(target: AdminRequester): void {
  statusTarget.value = target
  statusReasonError.value = ''
  statusVisible.value = true
}
async function submitStatus(reason: string): Promise<void> {
  const target = statusTarget.value
  if (!target || statusSubmitting.value) return
  statusSubmitting.value = true
  statusReasonError.value = ''
  try {
    await changeAdminRequesterStatus(target.id, {
      expectedUpdatedAt: target.updatedAt,
      status: target.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
      reason,
    })
    statusVisible.value = false
    statusTarget.value = null
    ElMessage.success(target.status === 'ACTIVE' ? '需求方账号已停用' : '需求方账号已启用')
    await loadRequesters()
  } catch (error) {
    if (['DATA_VERSION_CONFLICT', 'RESOURCE_NOT_FOUND'].includes(getApiErrorCode(error) ?? '')) {
      statusVisible.value = false
      statusTarget.value = null
      ElMessage.warning('账号信息已变化，请核对最新列表后重试')
      await loadRequesters()
    } else {
      statusReasonError.value = getApiFieldErrors(error).reason ?? ''
      ElMessage.error(getApiErrorMessage(error, '账号状态更新失败'))
    }
  } finally {
    statusSubmitting.value = false
  }
}
onMounted(() => void loadRequesters())
onUnmounted(() => {
  loadSequence++
})
</script>

<template>
  <section class="page">
    <AppPageHeader
      title="需求方账号"
      description="核实申请人身份后开通账号，管理需求方的访问状态。"
      eyebrow="ADMIN"
      :icon="UsersRound"
      tone="blue"
    >
      <template #meta
        ><span class="summary">{{
          errorMessage ? '列表暂不可用' : `共 ${total} 个需求方账号`
        }}</span></template
      >
      <template #actions
        ><el-button type="primary" @click="openCreate"
          ><UserPlus :size="16" aria-hidden="true" />开通账号</el-button
        ></template
      >
    </AppPageHeader>
    <el-alert
      type="info"
      :closable="false"
      title="受控开通不依赖自助注册开关；仅授予需求方权限。用户可在个人设置中维护资料和修改密码。"
    />
    <el-card class="filter-card" shadow="never">
      <el-form class="filters" label-position="top" @submit.prevent="search">
        <el-form-item label="关键词"
          ><el-input
            v-model="filters.keyword"
            maxlength="80"
            clearable
            placeholder="账号或显示名称"
        /></el-form-item>
        <el-form-item label="状态"
          ><el-select v-model="filters.status" clearable placeholder="全部状态"
            ><el-option label="已启用" value="ACTIVE" /><el-option
              label="已停用"
              value="DISABLED" /></el-select
        ></el-form-item>
        <div class="filter-actions">
          <el-button type="primary" native-type="submit"
            ><Search :size="16" aria-hidden="true" />查询</el-button
          ><el-button @click="resetFilters"
            ><RotateCcw :size="16" aria-hidden="true" />重置</el-button
          >
        </div>
      </el-form>
    </el-card>
    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage"
      ><template #default
        ><el-button link type="primary" @click="loadRequesters">重新加载</el-button></template
      ></el-alert
    >
    <el-card v-else class="result-card">
      <template #header
        ><div class="result-heading">
          <span>需求方清单</span><small>第 {{ page }} 页</small>
        </div></template
      >
      <div
        role="region"
        aria-label="需求方账号列表，可横向滚动查看完整信息"
        tabindex="0"
        class="table-region"
        :aria-busy="loading"
      >
        <el-table
          v-loading="loading"
          :data="items"
          row-key="id"
          :empty-text="loading ? '正在加载账号…' : '暂无符合条件的需求方账号'"
        >
          <el-table-column prop="account" label="账号" min-width="150" />
          <el-table-column prop="displayName" label="显示名称" min-width="130" />
          <el-table-column label="院系或部门" min-width="150"
            ><template #default="{ row }">{{ row.department ?? '—' }}</template></el-table-column
          >
          <el-table-column label="联系方式" min-width="210"
            ><template #default="{ row }"
              ><div>{{ row.email ?? '—' }}</div>
              <div class="secondary-text">{{ row.phone ?? '—' }}</div></template
            ></el-table-column
          >
          <el-table-column label="状态" width="100"
            ><template #default="{ row }"
              ><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{
                row.status === 'ACTIVE' ? '已启用' : '已停用'
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column label="开通时间" width="180"
            ><template #default="{ row }">{{
              formatDate(row.createdAt)
            }}</template></el-table-column
          >
          <el-table-column label="操作" width="100" fixed="right"
            ><template #default="{ row }"
              ><el-button
                link
                :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
                :disabled="loading || statusSubmitting"
                :aria-label="`${row.status === 'ACTIVE' ? '停用' : '启用'}${row.displayName}的账号`"
                @click="openStatus(row)"
                >{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button
              ></template
            ></el-table-column
          >
        </el-table>
      </div>
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="changePage"
        @size-change="changePageSize"
      />
    </el-card>
    <RequesterCreateDialog
      v-model="createVisible"
      :submitting="creating"
      :server-errors="createFieldErrors"
      :error-message="createError"
      @submit="submitCreate"
    />
    <AdminReasonDialog
      v-model="statusVisible"
      :title="statusTitle"
      :description="statusDescription"
      :confirm-text="statusTarget?.status === 'ACTIVE' ? '确认停用' : '确认启用'"
      :danger="statusTarget?.status === 'ACTIVE'"
      :submitting="statusSubmitting"
      :server-error="statusReasonError"
      @confirm="submitStatus"
    />
  </section>
</template>

<style scoped>
.summary,
.secondary-text,
.result-heading small {
  color: var(--color-text-tertiary);
}
.summary,
.result-heading small {
  font-size: 12px;
}
.filter-card {
  border-left: 3px solid var(--color-primary);
}
.filters {
  display: grid;
  grid-template-columns: minmax(200px, 2fr) minmax(150px, 1fr) auto;
  gap: 12px;
  align-items: end;
}
.filters :deep(.el-form-item) {
  margin-bottom: 0;
}
.filters :deep(.el-select) {
  width: 100%;
}
.filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.filter-actions :deep(.el-button) {
  margin: 0;
}
.filter-actions :deep(.el-button span) {
  display: flex;
  align-items: center;
  gap: 7px;
}
.result-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.result-heading > span {
  color: var(--color-text-primary);
  font-weight: 650;
}
.result-card :deep(.el-card__body) {
  padding: 0 0 16px;
}
.table-region {
  overflow-x: auto;
}
.table-region:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}
.pagination {
  justify-content: flex-end;
  margin-top: 16px;
  padding: 0 16px;
  overflow-x: auto;
}
@media (max-width: 800px) {
  .filters {
    grid-template-columns: 1fr;
  }
  .pagination {
    justify-content: flex-start;
  }
}
</style>
