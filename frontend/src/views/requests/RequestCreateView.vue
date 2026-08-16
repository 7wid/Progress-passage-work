<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getEnabledCategories } from '@/api/categories'
import { getApiErrorMessage, getApiStatus } from '@/api/http'
import {
  createDraft,
  createRequest,
  getRequestDetail,
  submitRequest,
  updateRequest,
} from '@/api/requests'
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

const editingId = computed(() => {
  const value = String(route.params.id ?? '')
  return /^[1-9]\d*$/.test(value) ? value : null
})
const isEditing = computed(() => editingId.value !== null)
const pageTitle = computed(() => {
  if (!isEditing.value) return '提交需求'
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
  categoriesLoading.value = true
  try {
    categories.value = await getEnabledCategories()
  } catch {
    ElMessage.error('需求分类加载失败，请确认后端已经启动')
  } finally {
    categoriesLoading.value = false
  }
}

async function loadExistingRequest() {
  if (!editingId.value) return
  initialLoading.value = true
  try {
    const detail = await getRequestDetail(editingId.value)
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
    ElMessage.error(getApiErrorMessage(error, '需求加载失败'))
    dirty.value = false
    await router.replace('/requests')
  } finally {
    initialLoading.value = false
  }
}

function disablePastDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

async function handleSave() {
  if (saving.value || submitting.value) return
  saving.value = true
  try {
    if (!editingId.value) {
      const created = await createDraft(form)
      dirty.value = false
      ElMessage.success('需求草稿已保存')
      await router.replace({ name: 'request-detail', params: { id: created.id } })
      return
    }
    if (requestVersion.value === null) return
    const updated = await updateRequest(editingId.value, form, requestVersion.value)
    requestVersion.value = updated.version
    dirty.value = false
    ElMessage.success(requestStatus.value === 'DRAFT' ? '需求草稿已保存' : '补充资料已保存')
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求已被其他操作更新，请返回详情后重试')
    } else {
      ElMessage.error(getApiErrorMessage(error, '需求保存失败'))
    }
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value || submitting.value || saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (!editingId.value) {
      const created = await createRequest(form)
      dirty.value = false
      ElMessage.success(`需求 ${created.requestNo} 提交成功`)
      await router.replace({ name: 'request-detail', params: { id: created.id } })
      return
    }
    if (requestVersion.value === null) return
    const updated = await updateRequest(editingId.value, form, requestVersion.value)
    const submitted = await submitRequest(editingId.value, updated.version)
    dirty.value = false
    ElMessage.success(`需求 ${submitted.requestNo} 提交成功`)
    await router.replace({ name: 'request-detail', params: { id: editingId.value } })
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求状态或版本已变化，请返回详情后重试')
    } else {
      ElMessage.error(getApiErrorMessage(error, '需求提交失败'))
    }
  } finally {
    submitting.value = false
  }
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

watch(
  form,
  () => {
    if (!hydrating.value) dirty.value = true
  },
  { deep: true },
)

onBeforeRouteLeave(async () => {
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
})

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await Promise.all([loadCategories(), loadExistingRequest()])
  hydrating.value = false
  dirty.value = false
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload))
</script>

<template>
  <section class="page">
    <div class="page__header">
      <h2>{{ pageTitle }}</h2>
    </div>

    <el-card v-loading="initialLoading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <div class="form-grid">
          <el-form-item label="需求标题" prop="title">
            <el-input v-model="form.title" maxlength="80" show-word-limit />
          </el-form-item>

          <el-form-item label="需求分类" prop="categoryId">
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

          <el-form-item label="期望完成日期" prop="expectedDeadline">
            <el-date-picker
              v-model="form.expectedDeadline"
              type="date"
              value-format="YYYY-MM-DD"
              :disabled-date="disablePastDate"
              class="form-control"
            />
          </el-form-item>

          <el-form-item label="紧急程度" prop="urgency">
            <el-radio-group v-model="form.urgency">
              <el-radio value="NORMAL">一般</el-radio>
              <el-radio value="HIGH">较急</el-radio>
              <el-radio value="URGENT">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="需求背景" prop="background">
          <el-input
            v-model="form.background"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="具体需求" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="7"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="期望成果" prop="expectedResult">
          <el-input
            v-model="form.expectedResult"
            type="textarea"
            :rows="4"
            maxlength="3000"
            show-word-limit
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="预算金额（可选）">
            <el-input
              v-model="form.budgetAmount"
              type="number"
              min="0"
              placeholder="例如：1000.00"
            />
          </el-form-item>
          <el-form-item label="预算说明（可选）">
            <el-input
              v-model="form.budgetDescription"
              maxlength="120"
              placeholder="例如：可沟通、无预算"
            />
          </el-form-item>
        </div>

        <el-form-item label="技术限制（可选）">
          <el-input
            v-model="form.technicalConstraints"
            type="textarea"
            :rows="3"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系方式" prop="contactInfo">
          <el-input
            v-model="form.contactInfo"
            maxlength="255"
            placeholder="手机号、邮箱或其他有效联系方式"
          />
        </el-form-item>

        <el-form-item prop="informationConfirmed">
          <el-checkbox v-model="form.informationConfirmed">
            我确认以上信息真实有效，且不包含违规或未授权数据
          </el-checkbox>
        </el-form-item>

        <div class="form-actions">
          <el-button @click="router.push(isEditing ? `/requests/${editingId}` : '/requests')">
            返回
          </el-button>
          <el-button :loading="saving" :disabled="initialLoading" @click="handleSave">
            {{ isEditing ? '保存修改' : '保存草稿' }}
          </el-button>
          <el-button
            type="primary"
            native-type="submit"
            :loading="submitting"
            :disabled="initialLoading"
          >
            {{ requestStatus === 'NEED_MORE_INFO' ? '重新提交' : '提交需求' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </section>
</template>

<style scoped>
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
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-wrap: wrap;
  }
}
</style>
