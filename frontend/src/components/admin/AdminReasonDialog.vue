<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    description: string
    confirmText: string
    submitting?: boolean
    serverError?: string
    danger?: boolean
  }>(),
  { submitting: false, serverError: '', danger: false },
)

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  confirm: [reason: string]
}>()

const formRef = ref<FormInstance>()
const form = reactive({ reason: '' })
const rules: FormRules<typeof form> = {
  reason: [
    { required: true, message: '请输入操作原因', trigger: 'blur' },
    { min: 5, max: 500, message: '操作原因应为 5～500 个字符', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    form.reason = ''
    void nextTick(() => formRef.value?.clearValidate())
  },
)

function close(): void {
  if (!props.submitting) emit('update:modelValue', false)
}

async function confirm(): Promise<void> {
  if (props.submitting) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('confirm', form.reason.trim())
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="520px"
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @close="close"
  >
    <el-alert
      :type="danger ? 'error' : 'warning'"
      :closable="false"
      :title="description"
      show-icon
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="reason-form">
      <el-form-item label="操作原因" prop="reason" :error="serverError">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请填写可供后续审计和追溯的具体原因"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button :type="danger ? 'danger' : 'primary'" :loading="submitting" @click="confirm">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.reason-form {
  margin-top: 16px;
}
</style>
