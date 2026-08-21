<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { isValidCategorySortOrder } from './adminFormValidation'
import type { AdminCategory, AdminCategoryEditorValue } from '@/types/admin'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    category: AdminCategory | null
    submitting?: boolean
    serverErrors?: Record<string, string>
  }>(),
  { submitting: false, serverErrors: () => ({}) },
)

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [value: AdminCategoryEditorValue]
}>()

const editing = computed(() => props.category !== null)
const formRef = ref<FormInstance>()
const form = reactive<AdminCategoryEditorValue>({ name: '', sortOrder: 0, reason: '' })
const rules: FormRules<AdminCategoryEditorValue> = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 80, message: '分类名称应为 2～80 个字符', trigger: 'blur' },
  ],
  sortOrder: [
    { required: true, message: '请输入排序值', trigger: 'change' },
    {
      validator: (_rule, value, callback) =>
        isValidCategorySortOrder(value)
          ? callback()
          : callback(new Error('排序值应为 0～9999 的整数')),
      trigger: ['blur', 'change'],
    },
  ],
  reason: [
    { required: true, message: '请输入操作原因', trigger: 'blur' },
    { min: 5, max: 500, message: '操作原因应为 5～500 个字符', trigger: 'blur' },
  ],
}

function resetForm(): void {
  form.name = props.category?.name ?? ''
  form.sortOrder = props.category?.sortOrder ?? 0
  form.reason = ''
  void nextTick(() => formRef.value?.clearValidate())
}

watch([() => props.modelValue, () => props.category], ([visible]) => {
  if (visible) resetForm()
})

function close(): void {
  if (!props.submitting) emit('update:modelValue', false)
}

async function submit(): Promise<void> {
  if (props.submitting) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', { name: form.name, sortOrder: form.sortOrder, reason: form.reason })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="editing ? '编辑分类' : '新建分类'"
    width="520px"
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @close="close"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="分类名称" prop="name" :error="serverErrors.name">
        <el-input v-model="form.name" maxlength="80" />
      </el-form-item>

      <el-form-item label="排序值" prop="sortOrder" :error="serverErrors.sortOrder">
        <el-input-number v-model="form.sortOrder" :min="0" :max="9999" :precision="0" :step="10" />
        <span class="field-help">数值越小，发起需求时显示越靠前。</span>
      </el-form-item>

      <el-form-item label="操作原因" prop="reason" :error="serverErrors.reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请说明新增或调整分类的原因"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ editing ? '保存修改' : '创建分类' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.field-help {
  margin-left: 10px;
  color: var(--color-text-tertiary);
  font-size: 12px;
}
</style>
