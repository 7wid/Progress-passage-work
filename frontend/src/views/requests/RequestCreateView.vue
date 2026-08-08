<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getEnabledCategories } from '@/api/categories'
import { createRequest } from '@/api/requests'
import type { CategoryOption, CreateRequestInput } from '@/types/request'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const categoriesLoading = ref(false)
const categories = ref<CategoryOption[]>([])

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
  categoryId: [
    {
      required: true,
      message: '请选择需求分类',
      trigger: 'change',
    },
  ],
  title: [
    {
      required: true,
      message: '请输入需求标题',
      trigger: 'blur',
    },
    {
      min: 5,
      max: 80,
      message: '标题长度应为 5～80 个字符',
      trigger: 'blur',
    },
  ],
  background: [
    {
      required: true,
      message: '请输入需求背景',
      trigger: 'blur',
    },
    {
      min: 20,
      max: 1000,
      message: '需求背景长度应为 20～1000 个字符',
      trigger: 'blur',
    },
  ],
  description: [
    {
      required: true,
      message: '请输入具体需求',
      trigger: 'blur',
    },
    {
      min: 50,
      max: 5000,
      message: '具体需求长度应为 50～5000 个字符',
      trigger: 'blur',
    },
  ],
  expectedResult: [
    {
      required: true,
      message: '请输入期望成果',
      trigger: 'blur',
    },
    {
      min: 5,
      max: 3000,
      message: '期望成果长度应为 5～3000 个字符',
      trigger: 'blur',
    },
  ],
  expectedDeadline: [
    {
      required: true,
      message: '请选择期望完成日期',
      trigger: 'change',
    },
  ],
  urgency: [
    {
      required: true,
      message: '请选择紧急程度',
      trigger: 'change',
    },
  ],
  contactInfo: [
    {
      required: true,
      message: '请输入联系方式',
      trigger: 'blur',
    },
    {
      max: 255,
      message: '联系方式不能超过 255 个字符',
      trigger: 'blur',
    },
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

function disablePastDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return date.getTime() < today.getTime()
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)

  if (!valid) {
    return
  }

  submitting.value = true

  try {
    const created = await createRequest({
      ...form,
      title: form.title.trim(),
      background: form.background.trim(),
      description: form.description.trim(),
      expectedResult: form.expectedResult.trim(),
      contactInfo: form.contactInfo.trim(),
    })

    ElMessage.success(`需求 ${created.requestNo} 提交成功`)

    await router.replace(`/requests/${created.id}`)
  } catch {
    ElMessage.error('需求提交失败，请检查表单和后端日志')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCategories)
</script>

<template>
  <section class="page">
    <div class="page__header">
      <h2>提交需求</h2>
    </div>

    <el-card>
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
          <el-button @click="router.push('/requests')"> 返回列表 </el-button>

          <el-button type="primary" native-type="submit" :loading="submitting">
            提交需求
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
}
</style>
