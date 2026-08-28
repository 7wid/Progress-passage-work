<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getInitialPasswordValidationMessage } from './adminFormValidation'
import type { CreateAdminRequesterInput } from '@/types/adminRequester'

const props = withDefaults(defineProps<{
  modelValue: boolean
  submitting?: boolean
  serverErrors?: Record<string, string>
  errorMessage?: string
}>(), { submitting: false, serverErrors: () => ({}), errorMessage: '' })

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [value: CreateAdminRequesterInput]
}>()
const formRef = ref<FormInstance>()
const emptyForm = (): CreateAdminRequesterInput => ({
  account: '', initialPassword: '', displayName: '', email: '', phone: '', department: '', reason: '',
})
const form = reactive(emptyForm())
const rules: FormRules<CreateAdminRequesterInput> = {
  account: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]{3,64}$/, message: '账号应为 3～64 位字母、数字、点、下划线或短横线', trigger: 'blur' },
  ],
  initialPassword: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { validator: (_rule, value: string, callback) => {
      const message = getInitialPasswordValidationMessage(value)
      callback(message ? new Error(message) : undefined)
    }, trigger: 'blur' },
  ],
  displayName: [
    { required: true, whitespace: true, message: '请输入显示名称', trigger: 'blur' },
    { min: 2, max: 80, message: '显示名称应为 2～80 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [{ pattern: /^[0-9+()\-\s]{6,32}$/, message: '联系电话格式不正确', trigger: 'blur' }],
  reason: [
    { required: true, whitespace: true, message: '请输入开通原因', trigger: 'blur' },
    { min: 5, max: 500, message: '开通原因应为 5～500 个字符', trigger: 'blur' },
  ],
}

watch(() => props.modelValue, (visible) => {
  // Clear credentials on every close, including a successful parent-driven close.
  Object.assign(form, emptyForm())
  if (visible) void nextTick(() => formRef.value?.clearValidate())
}, { immediate: true })

function close(): void {
  if (!props.submitting) emit('update:modelValue', false)
}

async function submit(): Promise<void> {
  if (props.submitting) return
  form.account = form.account.trim()
  form.displayName = form.displayName.trim()
  form.email = form.email.trim()
  form.phone = form.phone.trim()
  form.reason = form.reason.trim()
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid && !props.submitting) emit('submit', { ...form })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="开通需求方账号"
    width="min(680px, calc(100vw - 32px))"
    destroy-on-close
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @close="close"
  >
    <el-alert title="账号仅用于提交与跟进本人需求，不具备团队或管理权限。" type="info" :closable="false" />
    <p class="password-guidance">请先核实申请人身份，通过安全渠道单独告知初始密码，并提醒首次登录后在“个人设置”中修改。不要把密码填写在开通原因中。</p>
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" class="submit-error" />
    <el-form ref="formRef" :model="form" :rules="rules" :disabled="submitting" label-position="top" scroll-to-error @submit.prevent="submit">
      <div class="form-grid">
        <el-form-item label="登录账号" prop="account" :error="serverErrors.account">
          <el-input v-model="form.account" maxlength="64" autocomplete="off" placeholder="例如 requester01" />
        </el-form-item>
        <el-form-item label="初始密码" prop="initialPassword" :error="serverErrors.initialPassword">
          <el-input v-model="form.initialPassword" type="password" show-password maxlength="72" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName" :error="serverErrors.displayName">
          <el-input v-model="form.displayName" maxlength="80" />
        </el-form-item>
        <el-form-item label="院系或部门（选填）" prop="department" :error="serverErrors.department">
          <el-input v-model="form.department" maxlength="160" />
        </el-form-item>
        <el-form-item label="邮箱（选填）" prop="email" :error="serverErrors.email">
          <el-input v-model="form.email" maxlength="160" autocomplete="email" />
        </el-form-item>
        <el-form-item label="联系电话（选填）" prop="phone" :error="serverErrors.phone">
          <el-input v-model="form.phone" maxlength="32" autocomplete="tel" />
        </el-form-item>
      </div>
      <el-form-item label="开通原因" prop="reason" :error="serverErrors.reason">
        <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明申请来源与开通用途，供后续审计追溯" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认开通</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.password-guidance {
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin: 12px 0 20px;
}
.submit-error { margin-bottom: 16px; }
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
@media (max-width: 640px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
