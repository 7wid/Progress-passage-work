<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeAdminMemberStatus,
  createAdminMember,
  getAdminMember,
  getAdminMembers,
  getAdminSkillTags,
  updateAdminMember,
} from '@/api/adminMembers'
import { getApiErrorCode, getApiErrorMessage, getApiFieldErrors } from '@/api/http'
import AdminReasonDialog from '@/components/admin/AdminReasonDialog.vue'
import MemberEditorDialog from '@/components/admin/MemberEditorDialog.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  AdminMember,
  AdminMemberEditorValue,
  AdminMemberRole,
  AdminMemberStatus,
  SkillTag,
} from '@/types/admin'

const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const items = ref<AdminMember[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
let loadSequence = 0

const skillTags = ref<SkillTag[]>([])
const skillsLoading = ref(false)
const skillsError = ref('')

const filters = reactive<{
  keyword: string
  role?: AdminMemberRole
  status?: AdminMemberStatus
}>({ keyword: '', role: undefined, status: undefined })

const editorVisible = ref(false)
const editingMember = ref<AdminMember | null>(null)
const editingLoadId = ref<string | null>(null)
const editorSubmitting = ref(false)
const editorErrors = reactive<Record<string, string>>({})

const statusDialogVisible = ref(false)
const statusTarget = ref<AdminMember | null>(null)
const statusSubmitting = ref(false)
const statusReasonError = ref('')

const statusDialogTitle = computed(() =>
  statusTarget.value?.status === 'ACTIVE' ? '停用成员账号' : '启用成员账号',
)
const statusDialogDescription = computed(() => {
  const member = statusTarget.value
  if (!member) return ''
  return member.status === 'ACTIVE'
    ? `停用“${member.displayName}”后，该账号现有登录会话将失效，且不能继续处理需求。`
    : `启用“${member.displayName}”后，该账号可以重新登录系统。`
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

async function loadMembers(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getAdminMembers({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword,
      role: filters.role,
      status: filters.status,
    })
    if (sequence !== loadSequence) return
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (sequence !== loadSequence) return
    items.value = []
    total.value = 0
    errorMessage.value = getApiErrorMessage(error, '成员列表加载失败，请稍后重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

async function loadSkills(): Promise<void> {
  skillsLoading.value = true
  skillsError.value = ''
  try {
    skillTags.value = await getAdminSkillTags()
  } catch (error) {
    skillTags.value = []
    skillsError.value = getApiErrorMessage(error, '技能标签加载失败，请稍后重试')
  } finally {
    skillsLoading.value = false
  }
}

function search(): void {
  page.value = 1
  void loadMembers()
}

function resetFilters(): void {
  filters.keyword = ''
  filters.role = undefined
  filters.status = undefined
  page.value = 1
  void loadMembers()
}

function changePage(value: number): void {
  page.value = value
  void loadMembers()
}

function changePageSize(value: number): void {
  pageSize.value = value
  page.value = 1
  void loadMembers()
}

function openCreate(): void {
  editingMember.value = null
  clearErrors(editorErrors)
  editorVisible.value = true
}

async function openEdit(member: AdminMember): Promise<void> {
  if (editingLoadId.value !== null) return
  editingLoadId.value = member.id
  clearErrors(editorErrors)
  try {
    editingMember.value = await getAdminMember(member.id)
    editorVisible.value = true
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '成员详情加载失败，请稍后重试'))
  } finally {
    editingLoadId.value = null
  }
}

async function submitEditor(value: AdminMemberEditorValue): Promise<void> {
  if (editorSubmitting.value) return

  const member = editingMember.value
  const createsAdmin = member === null && value.role === 'ADMIN'
  const changesRole = member !== null && member.role !== value.role
  if (createsAdmin || changesRole) {
    try {
      const confirmationMessage = createsAdmin
        ? `确认创建管理员账号“${value.displayName.trim()}”吗？该账号将拥有成员、分类和异常需求管理权限。`
        : member
          ? `确认将“${member.displayName}”的角色由${member.role === 'ADMIN' ? '管理员' : '技术组成员'}调整为${value.role === 'ADMIN' ? '管理员' : '技术组成员'}吗？`
          : ''
      await ElMessageBox.confirm(
        confirmationMessage,
        createsAdmin ? '确认创建管理员' : '确认角色变更',
        {
          type: 'warning',
          confirmButtonText: createsAdmin ? '确认创建' : '确认变更',
          cancelButtonText: '返回检查',
        },
      )
    } catch {
      return
    }
  }

  editorSubmitting.value = true
  clearErrors(editorErrors)
  try {
    if (member) {
      await updateAdminMember(member.id, {
        expectedUpdatedAt: member.updatedAt,
        displayName: value.displayName,
        email: value.email,
        phone: value.phone,
        department: value.department,
        role: value.role,
        skillIds: value.skillIds,
        reason: value.reason,
      })
      ElMessage.success('成员信息已更新')
    } else {
      await createAdminMember(value)
      ElMessage.success('成员账号已创建，请通过安全渠道告知初始密码')
    }
    editorVisible.value = false
    editingMember.value = null
    await loadMembers()
  } catch (error) {
    if (getApiErrorCode(error) === 'DATA_VERSION_CONFLICT') {
      ElMessage.warning(getApiErrorMessage(error, '成员数据已变化，请使用最新信息重试'))
      editorVisible.value = false
      editingMember.value = null
      await loadMembers()
    } else {
      Object.assign(editorErrors, getApiFieldErrors(error))
      ElMessage.error(getApiErrorMessage(error, '成员保存失败，请检查填写内容'))
    }
  } finally {
    editorSubmitting.value = false
  }
}

function canDisable(member: AdminMember): boolean {
  return member.id !== authStore.user?.id && member.activeOwnerRequestCount === 0
}

function statusDisabledReason(member: AdminMember): string {
  if (member.status !== 'ACTIVE') return ''
  if (member.id === authStore.user?.id) return '不能停用当前登录账号'
  if (member.activeOwnerRequestCount > 0) return '请先转交该成员负责的处理中需求'
  return ''
}

function openStatusDialog(member: AdminMember): void {
  if (member.status === 'ACTIVE' && !canDisable(member)) {
    ElMessage.warning(statusDisabledReason(member))
    return
  }
  statusTarget.value = member
  statusReasonError.value = ''
  statusDialogVisible.value = true
}

async function submitStatus(reason: string): Promise<void> {
  const member = statusTarget.value
  if (!member || statusSubmitting.value) return
  statusSubmitting.value = true
  statusReasonError.value = ''
  try {
    await changeAdminMemberStatus(member.id, {
      expectedUpdatedAt: member.updatedAt,
      status: member.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
      reason,
    })
    statusDialogVisible.value = false
    statusTarget.value = null
    ElMessage.success(member.status === 'ACTIVE' ? '成员账号已停用' : '成员账号已启用')
    await loadMembers()
  } catch (error) {
    const fields = getApiFieldErrors(error)
    statusReasonError.value = fields.reason ?? ''
    if (getApiErrorCode(error) === 'DATA_VERSION_CONFLICT') {
      ElMessage.warning(getApiErrorMessage(error, '成员状态已变化，请使用最新数据重试'))
      statusDialogVisible.value = false
      statusTarget.value = null
      await loadMembers()
    } else {
      ElMessage.error(getApiErrorMessage(error, '成员状态更新失败'))
    }
  } finally {
    statusSubmitting.value = false
  }
}

onMounted(() => {
  void loadMembers()
  void loadSkills()
})
</script>

<template>
  <section class="page">
    <div class="page__header">
      <div>
        <h2>成员管理</h2>
        <span class="summary">共 {{ total }} 个技术组账号</span>
      </div>
      <el-button
        type="primary"
        :disabled="skillsLoading || Boolean(skillsError)"
        @click="openCreate"
      >
        新建成员
      </el-button>
    </div>

    <el-alert v-if="skillsError" type="warning" :closable="false" :title="skillsError">
      <template #default>
        <el-button link type="primary" @click="loadSkills">重新加载技能标签</el-button>
      </template>
    </el-alert>

    <el-card>
      <el-form class="filters" label-position="top" @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            maxlength="80"
            clearable
            placeholder="账号或显示名称"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.role" clearable placeholder="全部角色">
            <el-option label="技术组成员" value="MEMBER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option label="已启用" value="ACTIVE" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <div class="filter-actions">
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadMembers">重新加载</el-button>
      </template>
    </el-alert>

    <el-card>
      <el-table v-loading="loading" :data="items" row-key="id" empty-text="暂无成员账号">
        <el-table-column prop="account" label="账号" min-width="130" />
        <el-table-column prop="displayName" label="显示名称" min-width="120" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '技术组成员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="技能" min-width="190">
          <template #default="{ row }">
            <div v-if="row.skills.length" class="tag-list">
              <el-tag v-for="skill in row.skills" :key="skill.id" size="small" type="info">
                {{ skill.name }}
              </el-tag>
            </div>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="180">
          <template #default="{ row }">
            <div>{{ row.email ?? '—' }}</div>
            <div class="secondary-text">{{ row.phone ?? '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="院系/部门" min-width="130">
          <template #default="{ row }">{{ row.department ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="负责中" width="90" align="center">
          <template #default="{ row }">{{ row.activeOwnerRequestCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :loading="editingLoadId === row.id"
              :disabled="skillsLoading || Boolean(skillsError) || editingLoadId !== null"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-tooltip
              :disabled="!statusDisabledReason(row)"
              :content="statusDisabledReason(row)"
              placement="top"
            >
              <span>
                <el-button
                  link
                  :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                  :disabled="row.status === 'ACTIVE' && !canDisable(row)"
                  @click="openStatusDialog(row)"
                >
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </el-button>
              </span>
            </el-tooltip>
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
        @current-change="changePage"
        @size-change="changePageSize"
      />
    </el-card>

    <MemberEditorDialog
      v-model="editorVisible"
      :member="editingMember"
      :skill-tags="skillTags"
      :submitting="editorSubmitting"
      :server-errors="editorErrors"
      @submit="submitEditor"
    />

    <AdminReasonDialog
      v-model="statusDialogVisible"
      :title="statusDialogTitle"
      :description="statusDialogDescription"
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
.secondary-text {
  color: #6b7280;
}

.filters {
  display: grid;
  grid-template-columns: minmax(220px, 2fr) repeat(2, minmax(150px, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.filters :deep(.el-select) {
  width: 100%;
}

.filter-actions,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 900px) {
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
