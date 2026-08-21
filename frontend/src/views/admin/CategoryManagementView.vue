<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshCw, Search, Tags } from '@lucide/vue'
import {
  changeAdminCategoryStatus,
  createAdminCategory,
  getAdminCategories,
  updateAdminCategory,
} from '@/api/adminCategories'
import { getApiErrorCode, getApiErrorMessage, getApiFieldErrors } from '@/api/http'
import AdminReasonDialog from '@/components/admin/AdminReasonDialog.vue'
import CategoryEditorDialog from '@/components/admin/CategoryEditorDialog.vue'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import type { AdminCategory, AdminCategoryEditorValue } from '@/types/admin'

type CategoryStatusFilter = 'ALL' | 'ENABLED' | 'DISABLED'

const loading = ref(false)
const errorMessage = ref('')
const items = ref<AdminCategory[]>([])
const keyword = ref('')
const statusFilter = ref<CategoryStatusFilter>('ALL')
let loadSequence = 0

const editorVisible = ref(false)
const editingCategory = ref<AdminCategory | null>(null)
const editorSubmitting = ref(false)
const editorErrors = reactive<Record<string, string>>({})

const statusDialogVisible = ref(false)
const statusTarget = ref<AdminCategory | null>(null)
const statusSubmitting = ref(false)
const statusReasonError = ref('')

const filteredItems = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLocaleLowerCase('zh-CN')
  return items.value.filter((category) => {
    if (statusFilter.value === 'ENABLED' && !category.enabled) return false
    if (statusFilter.value === 'DISABLED' && category.enabled) return false
    return (
      !normalizedKeyword || category.name.toLocaleLowerCase('zh-CN').includes(normalizedKeyword)
    )
  })
})

const statusDialogTitle = computed(() =>
  statusTarget.value?.enabled ? '停用需求分类' : '启用需求分类',
)
const statusDialogDescription = computed(() => {
  const category = statusTarget.value
  if (!category) return ''
  return category.enabled
    ? `停用“${category.name}”后，新建需求将无法再选择它；已有历史需求不会被删除。`
    : `启用“${category.name}”后，新建需求可以重新选择它。`
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function clearErrors(target: Record<string, string>): void {
  Object.keys(target).forEach((key) => delete target[key])
}

async function loadCategories(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getAdminCategories()
    if (sequence !== loadSequence) return
    items.value = result
  } catch (error) {
    if (sequence !== loadSequence) return
    items.value = []
    errorMessage.value = getApiErrorMessage(error, '分类列表加载失败，请稍后重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function openCreate(): void {
  editingCategory.value = null
  clearErrors(editorErrors)
  editorVisible.value = true
}

function openEdit(category: AdminCategory): void {
  editingCategory.value = category
  clearErrors(editorErrors)
  editorVisible.value = true
}

async function submitEditor(value: AdminCategoryEditorValue): Promise<void> {
  if (editorSubmitting.value) return
  editorSubmitting.value = true
  clearErrors(editorErrors)
  const category = editingCategory.value
  try {
    if (category) {
      await updateAdminCategory(category.id, {
        expectedUpdatedAt: category.updatedAt,
        name: value.name,
        sortOrder: value.sortOrder,
        reason: value.reason,
      })
      ElMessage.success('分类信息已更新')
    } else {
      await createAdminCategory(value)
      ElMessage.success('分类已创建')
    }
    editorVisible.value = false
    editingCategory.value = null
    await loadCategories()
  } catch (error) {
    if (getApiErrorCode(error) === 'DATA_VERSION_CONFLICT') {
      ElMessage.warning(getApiErrorMessage(error, '分类数据已变化，请使用最新数据重试'))
      editorVisible.value = false
      editingCategory.value = null
      await loadCategories()
    } else {
      Object.assign(editorErrors, getApiFieldErrors(error))
      ElMessage.error(getApiErrorMessage(error, '分类保存失败，请检查填写内容'))
    }
  } finally {
    editorSubmitting.value = false
  }
}

function openStatusDialog(category: AdminCategory): void {
  statusTarget.value = category
  statusReasonError.value = ''
  statusDialogVisible.value = true
}

async function submitStatus(reason: string): Promise<void> {
  const category = statusTarget.value
  if (!category || statusSubmitting.value) return
  statusSubmitting.value = true
  statusReasonError.value = ''
  try {
    await changeAdminCategoryStatus(category.id, {
      expectedUpdatedAt: category.updatedAt,
      enabled: !category.enabled,
      reason,
    })
    statusDialogVisible.value = false
    statusTarget.value = null
    ElMessage.success(category.enabled ? '分类已停用' : '分类已启用')
    await loadCategories()
  } catch (error) {
    const fields = getApiFieldErrors(error)
    statusReasonError.value = fields.reason ?? ''
    if (getApiErrorCode(error) === 'DATA_VERSION_CONFLICT') {
      ElMessage.warning(getApiErrorMessage(error, '分类状态已变化，请使用最新数据重试'))
      statusDialogVisible.value = false
      statusTarget.value = null
      await loadCategories()
    } else {
      ElMessage.error(getApiErrorMessage(error, '分类状态更新失败'))
    }
  } finally {
    statusSubmitting.value = false
  }
}

onMounted(() => void loadCategories())
</script>

<template>
  <section class="page">
    <AppPageHeader
      title="分类管理"
      description="维护需求分类、显示顺序与可用状态，列表包含已停用分类。"
      eyebrow="ADMIN"
      :icon="Tags"
      tone="orange"
    >
      <template #meta
        ><span class="summary">共 {{ items.length }} 个分类</span></template
      >
      <template #actions>
        <el-button type="primary" @click="openCreate">
          <Plus :size="16" aria-hidden="true" />新建分类
        </el-button>
      </template>
    </AppPageHeader>

    <el-card class="filter-card" shadow="never">
      <div class="filters">
        <el-input v-model="keyword" clearable maxlength="80" placeholder="按分类名称筛选">
          <template #prefix><Search :size="16" aria-hidden="true" /></template>
        </el-input>
        <el-select v-model="statusFilter">
          <el-option label="全部状态" value="ALL" />
          <el-option label="已启用" value="ENABLED" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button :loading="loading" @click="loadCategories">
          <RefreshCw :size="16" aria-hidden="true" />刷新
        </el-button>
      </div>
    </el-card>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadCategories">重新加载</el-button>
      </template>
    </el-alert>

    <el-card class="result-card">
      <template #header>
        <div class="result-heading">
          <span><Tags :size="17" aria-hidden="true" />分类清单</span>
          <small>当前显示 {{ filteredItems.length }} 项</small>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="filteredItems"
        row-key="id"
        empty-text="暂无符合条件的分类"
      >
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="sortOrder" label="排序值" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.enabled ? 'danger' : 'success'"
              @click="openStatusDialog(row)"
            >
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <CategoryEditorDialog
      v-model="editorVisible"
      :category="editingCategory"
      :submitting="editorSubmitting"
      :server-errors="editorErrors"
      @submit="submitEditor"
    />

    <AdminReasonDialog
      v-model="statusDialogVisible"
      :title="statusDialogTitle"
      :description="statusDialogDescription"
      :confirm-text="statusTarget?.enabled ? '确认停用' : '确认启用'"
      :danger="statusTarget?.enabled"
      :submitting="statusSubmitting"
      :server-error="statusReasonError"
      @confirm="submitStatus"
    />
  </section>
</template>

<style scoped>
.summary {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.filter-card {
  border-left: 3px solid var(--color-warning);
}

.filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto;
  gap: 12px;
}

.filters :deep(.el-select) {
  width: 100%;
}

.filters :deep(.el-button span),
.result-heading,
.result-heading > span {
  display: flex;
  align-items: center;
}

.filters :deep(.el-button span),
.result-heading > span {
  gap: 7px;
}

.result-heading {
  justify-content: space-between;
  gap: 16px;
}

.result-heading > span {
  color: var(--color-text-primary);
  font-weight: 650;
}

.result-heading small {
  color: var(--color-text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.result-card :deep(.el-card__body) {
  padding: 0;
}

@media (max-width: 640px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
