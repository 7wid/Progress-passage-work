<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  onBeforeRouteLeave,
  onBeforeRouteUpdate,
  RouterLink,
  useRoute,
  useRouter,
} from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, FilePenLine, Save, Send } from '@lucide/vue'
import { getEnabledCategories } from '@/api/categories'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import {
  createDraft,
  createRequest,
  getRequestDetail,
  submitRequest,
  updateRequest,
} from '@/api/requests'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import RequestSupplementGuide from '@/components/requests/RequestSupplementGuide.vue'
import type { CategoryOption, CreateRequestInput, RequestStatus } from '@/types/request'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const saving = ref(false)
const initialLoading = ref(false)
const categoriesLoading = ref(false)
const categories = ref<CategoryOption[]>([])
const requestVersion = ref<number | null>(null)
const requestStatus = ref<RequestStatus | null>(null)
const dirty = ref(false)
const hydrating = ref(true)
const categoriesError = ref(false)
const operationError = ref('')
const fieldErrors = ref<Record<string, string>>({})
const conflict = ref(false)
const saveFeedback = ref('')
const busy = computed(() => saving.value || submitting.value)
const formLocked = computed(() => busy.value || initialLoading.value || hydrating.value)
const cannotWrite = computed(
  () => formLocked.value || conflict.value || (isEditing.value && requestVersion.value === null),
)
let active = true
let navigatingAfterSuccess = false

const editingId = computed(() => {
  const value = String(route.params.id ?? '')
  return /^[1-9]\d*$/.test(value) ? value : null
})
const isEditing = computed(() => route.params.id !== undefined)
const pageTitle = computed(() => {
  if (!isEditing.value) return '发起新需求'
  return requestStatus.value === 'NEED_MORE_INFO' ? '补充需求资料' : '编辑需求草稿'
})

const form = reactive<CreateRequestInput>({
  categoryId: '',
  title: '',
  background: '',
  description: '',
  expectedResult: '',
  expectedDeadline: '',
  urgency: 'NORMAL',
  budgetAmount: '',
  budgetDescription: '',
  technicalConstraints: '',
  contactInfo: '',
  informationConfirmed: false,
})

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择需求分类', trigger: 'change' }],
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 5, max: 80, message: '标题长度应为 5～80 个字符', trigger: 'blur' },
  ],
  background: [
    { required: true, message: '请输入需求背景', trigger: 'blur' },
    { min: 20, max: 1000, message: '需求背景长度应为 20～1000 个字符', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请输入具体需求', trigger: 'blur' },
    { min: 50, max: 5000, message: '具体需求长度应为 50～5000 个字符', trigger: 'blur' },
  ],
  expectedResult: [
    { required: true, message: '请输入期望成果', trigger: 'blur' },
    { min: 5, max: 3000, message: '期望成果长度应为 5～3000 个字符', trigger: 'blur' },
  ],
  expectedDeadline: [{ required: true, message: '请选择期望完成日期', trigger: 'change' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }],
  contactInfo: [
    { required: true, message: '请输入联系方式', trigger: 'blur' },
    { max: 255, message: '联系方式不能超过 255 个字符', trigger: 'blur' },
  ],
  informationConfirmed: [
    {
      validator: (_rule, value, callback) => {
        if (value !== true) {
          callback(new Error('请确认所填信息真实有效'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

async function loadCategories() {
  if (categoriesLoading.value) return
  categoriesLoading.value = true
  categoriesError.value = false
  try {
    categories.value = await getEnabledCategories()
  } catch {
    categoriesError.value = true
  } finally {
    categoriesLoading.value = false
  }
}

async function loadExistingRequest() {
  if (!isEditing.value) return
  if (!editingId.value) {
    ElMessage.error('需求编号无效，请从需求列表重新进入')
    await router.replace('/requests')
    return
  }
  initialLoading.value = true
  try {
    const detail = await getRequestDetail(editingId.value)
    if (!active) return
    if (detail.status !== 'DRAFT' && detail.status !== 'NEED_MORE_INFO') {
      ElMessage.warning('当前需求状态不允许修改')
      dirty.value = false
      await router.replace({ name: 'request-detail', params: { id: detail.id } })
      return
    }
    requestVersion.value = detail.version
    requestStatus.value = detail.status
    Object.assign(form, {
      categoryId: detail.categoryId ?? '',
      title: detail.title ?? '',
      background: detail.background ?? '',
      description: detail.description ?? '',
      expectedResult: detail.expectedResult ?? '',
      expectedDeadline: detail.expectedDeadline ?? '',
      urgency: detail.urgency ?? 'NORMAL',
      budgetAmount: detail.budgetAmount === null ? '' : String(detail.budgetAmount),
      budgetDescription: detail.budgetDescription ?? '',
      technicalConstraints: detail.technicalConstraints ?? '',
      contactInfo: detail.contactInfo ?? '',
      informationConfirmed: false,
    })
  } catch (error) {
    if (!active) return
    ElMessage.error(getApiErrorMessage(error, '需求加载失败'))
    dirty.value = false
    await router.replace('/requests')
  } finally {
    initialLoading.value = false
  }
}

async function openSavedRequest(id: string) {
  navigatingAfterSuccess = true
  try {
    await router.replace({ name: 'request-detail', params: { id } })
  } finally {
    navigatingAfterSuccess = false
  }
}

function showWriteError(error: unknown, fallback: string, contentSaved = false) {
  fieldErrors.value = getApiFieldErrors(error)
  if (getApiStatus(error) === 409) {
    conflict.value = true
    operationError.value =
      '需求状态或版本已变化，已停止继续保存和提交。请先复制需要保留的修改，再查看最新详情确认处理结果。'
    return
  }
  operationError.value = getApiErrorMessage(error, fallback)
  if (contentSaved) {
    operationError.value +=
      ' 本次填写内容已保存，但提交结果尚未确认。可重试提交，或查看最新详情确认是否已进入评估。'
  } else {
    operationError.value += ' 当前填写内容仍保留在本页，请确认后重试。'
  }
}

function disablePastDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

async function handleSave() {
  if (cannotWrite.value) return
  saving.value = true
  operationError.value = ''
  fieldErrors.value = {}
  const id = editingId.value
  const input = { ...form }
  try {
    if (!id) {
      const created = await createDraft(input)
      dirty.value = false
      ElMessage.success('需求草稿已保存')
      await openSavedRequest(created.id)
      return
    }
    if (requestVersion.value === null) return
    const updated = await updateRequest(id, input, requestVersion.value)
    requestVersion.value = updated.version
    dirty.value = false
    saveFeedback.value = '内容已保存，尚未提交。确认填写完整后，请点击下方提交按钮。'
    ElMessage.success(requestStatus.value === 'DRAFT' ? '需求草稿已保存' : '补充资料已保存')
  } catch (error) {
    showWriteError(error, '需求保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (
    !formRef.value ||
    cannotWrite.value ||
    categoriesLoading.value ||
    categoriesError.value ||
    !categories.value.length
  )
    return
  // Lock before asynchronous validation as well as during both write requests.
  submitting.value = true
  operationError.value = ''
  fieldErrors.value = {}
  saveFeedback.value = ''
  let contentSaved = false
  const id = editingId.value
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) {
      operationError.value = '请检查并修正标记的表单内容后再提交。'
      return
    }
    const input = { ...form }
    if (!id) {
      const created = await createRequest(input)
      dirty.value = false
      ElMessage.success(`需求 ${created.requestNo} 已成功发起`)
      await openSavedRequest(created.id)
      return
    }
    if (requestVersion.value === null) return
    const updated = await updateRequest(id, input, requestVersion.value)
    // Saving is a committed operation even if the subsequent submit fails.
    requestVersion.value = updated.version
    dirty.value = false
    contentSaved = true
    const submitted = await submitRequest(id, updated.version)
    requestVersion.value = submitted.version
    dirty.value = false
    ElMessage.success(
      requestStatus.value === 'NEED_MORE_INFO'
        ? `需求 ${submitted.requestNo} 的补充资料已提交`
        : `需求 ${submitted.requestNo} 已成功发起`,
    )
    await openSavedRequest(id)
  } catch (error) {
    showWriteError(
      error,
      requestStatus.value === 'NEED_MORE_INFO' ? '资料提交失败' : '需求发起失败',
      contentSaved,
    )
  } finally {
    submitting.value = false
  }
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!dirty.value && !busy.value) return
  event.preventDefault()
  event.returnValue = ''
}

watch(
  form,
  () => {
    if (!hydrating.value) {
      dirty.value = true
      saveFeedback.value = ''
    }
  },
  { deep: true },
)

async function confirmLeaving() {
  if (navigatingAfterSuccess) return true
  if (busy.value) {
    ElMessage.warning('正在保存或提交，请等待处理结果后再离开')
    return false
  }
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前修改尚未保存，确定离开吗？', '未保存的修改', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
}

onBeforeRouteLeave(confirmLeaving)
// The shell keys the editor by fullPath, so parameter/query changes also discard input.
onBeforeRouteUpdate(confirmLeaving)

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await Promise.all([loadCategories(), loadExistingRequest()])
  hydrating.value = false
  dirty.value = false
})

onBeforeUnmount(() => {
  active = false
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <section class="page">
    <AppPageHeader
      :title="pageTitle"
      description="说明背景、目标与约束，帮助服务团队准确评估并安排处理。"
      eyebrow="REQUEST FORM"
      :icon="FilePenLine"
    />

    <RequestSupplementGuide
      v-if="requestStatus === 'NEED_MORE_INFO' && editingId"
      :request-id="editingId"
      class="request-form-card"
    />

    <el-card v-loading="initialLoading" class="request-form-card">
      <div v-if="categoriesError" class="form-notice" role="alert">
        <p>需求分类加载失败，已填写的内容不会清空。请重试加载后提交，也可先保存草稿。</p>
        <el-button :loading="categoriesLoading" @click="loadCategories">重新加载分类</el-button>
      </div>
      <div
        v-else-if="!categoriesLoading && categories.length === 0"
        class="form-notice"
        role="status"
      >
        <p>暂无可用需求分类，请联系管理员启用分类后提交。你可以先保存草稿。</p>
        <el-button @click="loadCategories">重新加载分类</el-button>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :disabled="formLocked"
        :scroll-to-error="true"
        :aria-busy="busy"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <div class="form-section-heading">
          <span>01</span>
          <div>
            <strong>基础信息</strong>
            <small>用于分类、排期与优先级判断</small>
          </div>
        </div>
        <div class="form-grid">
          <el-form-item label="需求标题" prop="title" :error="fieldErrors.title">
            <el-input v-model="form.title" maxlength="80" show-word-limit />
          </el-form-item>

          <el-form-item label="需求分类" prop="categoryId" :error="fieldErrors.categoryId">
            <el-select
              v-model="form.categoryId"
              :loading="categoriesLoading"
              placeholder="请选择分类"
              class="form-control"
            >
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            label="期望完成日期"
            prop="expectedDeadline"
            :error="fieldErrors.expectedDeadline"
          >
            <el-date-picker
              v-model="form.expectedDeadline"
              type="date"
              value-format="YYYY-MM-DD"
              :disabled-date="disablePastDate"
              class="form-control"
            />
          </el-form-item>

          <el-form-item label="紧急程度" prop="urgency" :error="fieldErrors.urgency">
            <el-radio-group v-model="form.urgency">
              <el-radio value="NORMAL">一般</el-radio>
              <el-radio value="HIGH">较急</el-radio>
              <el-radio value="URGENT">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <div class="form-section-heading">
          <span>02</span>
          <div>
            <strong>需求说明</strong>
            <small>完整描述问题背景、具体需求和期望成果</small>
          </div>
        </div>
        <el-form-item label="需求背景" prop="background" :error="fieldErrors.background">
          <el-input
            v-model="form.background"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="具体需求" prop="description" :error="fieldErrors.description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="7"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="期望成果" prop="expectedResult" :error="fieldErrors.expectedResult">
          <el-input
            v-model="form.expectedResult"
            type="textarea"
            :rows="4"
            maxlength="3000"
            show-word-limit
          />
        </el-form-item>

        <div class="form-section-heading">
          <span>03</span>
          <div>
            <strong>约束与联系</strong>
            <small>补充预算、实施约束和有效联系方式</small>
          </div>
        </div>
        <div class="form-grid">
          <el-form-item
            label="预算金额（可选）"
            prop="budgetAmount"
            :error="fieldErrors.budgetAmount"
          >
            <el-input
              v-model="form.budgetAmount"
              type="number"
              min="0"
              placeholder="例如：1000.00"
            />
          </el-form-item>
          <el-form-item
            label="预算说明（可选）"
            prop="budgetDescription"
            :error="fieldErrors.budgetDescription"
          >
            <el-input
              v-model="form.budgetDescription"
              maxlength="120"
              placeholder="例如：可沟通、无预算"
            />
          </el-form-item>
        </div>

        <el-form-item
          label="实施约束（可选）"
          prop="technicalConstraints"
          :error="fieldErrors.technicalConstraints"
        >
          <el-input
            v-model="form.technicalConstraints"
            type="textarea"
            :rows="3"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系方式" prop="contactInfo" :error="fieldErrors.contactInfo">
          <el-input
            v-model="form.contactInfo"
            maxlength="255"
            placeholder="手机号、邮箱或其他有效联系方式"
          />
        </el-form-item>

        <el-form-item prop="informationConfirmed" :error="fieldErrors.informationConfirmed">
          <el-checkbox v-model="form.informationConfirmed">
            我确认以上信息真实有效，且不包含违规或未授权数据
          </el-checkbox>
        </el-form-item>

        <div v-if="operationError" class="form-notice form-notice--error" role="alert">
          <p>{{ operationError }}</p>
          <RouterLink v-if="editingId" :to="{ name: 'request-detail', params: { id: editingId } }">
            查看最新详情
          </RouterLink>
        </div>
        <p class="save-status" role="status" aria-live="polite">
          {{
            busy
              ? '正在处理，请勿关闭或离开页面…'
              : dirty
                ? '有未保存的修改，可先保存再继续填写。'
                : saveFeedback
          }}
        </p>
        <div class="form-actions">
          <el-button @click="router.push(isEditing ? `/requests/${editingId}` : '/requests')">
            <ArrowLeft :size="16" aria-hidden="true" />
            返回
          </el-button>
          <el-button :loading="saving" :disabled="cannotWrite" @click="handleSave">
            <Save :size="16" aria-hidden="true" />
            {{ isEditing ? '保存修改' : '保存草稿' }}
          </el-button>
          <el-button
            type="primary"
            native-type="submit"
            :loading="submitting"
            :disabled="cannotWrite || categoriesLoading || categoriesError || !categories.length"
          >
            <Send :size="16" aria-hidden="true" />
            {{ requestStatus === 'NEED_MORE_INFO' ? '提交补充资料' : '确认发起' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </section>
</template>

<style scoped>
.request-form-card {
  width: min(1060px, 100%);
}

.request-form-card :deep(.el-card__body) {
  padding: 26px 28px;
}

.form-notice {
  margin-bottom: 20px;
  padding: 16px;
  color: var(--color-text-primary);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow-wrap: anywhere;
}

.form-notice p {
  margin: 0 0 10px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.form-notice--error {
  background: var(--el-color-danger-light-9);
  border-color: var(--el-color-danger-light-7);
}

.form-notice a {
  color: var(--color-primary-strong);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.form-notice a:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 4px;
}

.save-status {
  min-height: 22px;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.form-section-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 4px 0 18px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border-subtle);
}

.form-section-heading:first-child {
  padding-top: 0;
  border-top: 0;
}

.form-section-heading > span {
  display: inline-grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.form-section-heading > div {
  display: grid;
  gap: 1px;
}

.form-section-heading strong {
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 650;
}

.form-section-heading small {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-control {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-subtle);
}

.form-actions :deep(.el-button) {
  margin: 0;
}

.form-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 768px) {
  .request-form-card :deep(.el-card__body) {
    padding: 20px 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
